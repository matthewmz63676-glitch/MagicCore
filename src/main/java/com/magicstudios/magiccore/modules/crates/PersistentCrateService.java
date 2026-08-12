package com.magicstudios.magiccore.modules.crates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;

public final class PersistentCrateService implements CrateService {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final Clock clock;
    private final Map<String, CrateDefinition> definitions;
    private final Map<String, CurrencyDefinition> currencies;
    private final String costCurrency;
    private final LongUnaryOperator randomBelow;
    private final RecordRepository<CrateKeyBalance> keys = new RecordRepository<>("crates.keys", CrateKeyBalance.class);
    private final RecordRepository<CrateOpenCount> counts = new RecordRepository<>("crates.counts", CrateOpenCount.class);
    private final RecordRepository<CrateOpening> openings = new RecordRepository<>("crates.openings", CrateOpening.class);

    public PersistentCrateService(TransactionalDataStore store, DomainEventBus events, Clock clock,
                                  Map<String, CrateDefinition> definitions,
                                  Map<String, CurrencyDefinition> currencies, String costCurrency) {
        this(store, events, clock, definitions, currencies, costCurrency,
                bound -> ThreadLocalRandom.current().nextLong(bound));
    }

    public PersistentCrateService(TransactionalDataStore store, DomainEventBus events, Clock clock,
                                  Map<String, CrateDefinition> definitions,
                                  Map<String, CurrencyDefinition> currencies, String costCurrency,
                                  LongUnaryOperator randomBelow) {
        this.store = store;
        this.events = events;
        this.clock = clock;
        this.definitions = Map.copyOf(definitions);
        this.currencies = Map.copyOf(currencies);
        this.costCurrency = costCurrency;
        this.randomBelow = randomBelow;
        requireCurrency(costCurrency);
        this.definitions.values().forEach(this::validate);
    }

    @Override public Map<String, CrateDefinition> definitions() { return definitions; }

    @Override public CompletionStage<CrateKeyBalance> keyBalance(UUID playerId, String keyId) {
        return store.read(reader -> loadKey(reader, playerId, keyId));
    }

    @Override public CompletionStage<CrateKeyBalance> grantKeys(UUID playerId, String keyId, long amount, String operationKey) {
        if (amount < 1) throw new IllegalArgumentException("key amount must be positive");
        return store.transact("crate-key-grant:" + operationKey, transaction -> {
            CrateKeyBalance before = loadKey(transaction, playerId, keyId);
            if (!IdempotencyKeys.reserve(transaction, "crate-key-grant", operationKey)) return before;
            return putKey(transaction, new CrateKeyBalance(playerId, keyId, Math.addExact(before.amount(), amount), clock.instant()));
        }).thenApply(result->{events.publish(new CrateKeysChanged(playerId,keyId,result.amount(),operationKey,clock.instant()));return result;});
    }

    @Override public CompletionStage<CrateOpenResult> open(UUID playerId, String crateId, int amount, String operationKey) {
        CrateDefinition definition = requireDefinition(crateId);
        if (amount < 1 || amount > definition.maximumOpenAmount())
            throw new IllegalArgumentException("open amount must be 1.." + definition.maximumOpenAmount());
        return store.transact("crate-open:" + operationKey, transaction -> {
            var replay = openings.get(transaction, operationKey);
            if (replay.isPresent()) return new CrateOpenResult(false, "REPLAY", replay.get().value());
            if (!IdempotencyKeys.reserve(transaction, "crate-open", operationKey))
                throw new IllegalStateException("CRATE_OPERATION_WITHOUT_RESULT");
            long totalCost = Math.multiplyExact(definition.cost().amount(), amount);
            if (definition.cost().type() == CrateCost.Type.KEY) {
                CrateKeyBalance before = loadKey(transaction, playerId, definition.cost().keyId());
                if (before.amount() < totalCost) throw new IllegalStateException("INSUFFICIENT_KEYS");
                putKey(transaction, new CrateKeyBalance(playerId, before.keyId(), before.amount() - totalCost, clock.instant()));
            } else {
                EconomyTransactionSupport.credit(transaction, playerId, requireCurrency(costCurrency), -totalCost,
                        operationKey + ":cost", "crate:" + crateId, "crate-opening", clock.instant());
            }

            CrateOpenCount beforeCount = loadCount(transaction, playerId, crateId);
            long afterCount = Math.addExact(beforeCount.count(), amount);
            List<GrantedCrateReward> granted = new ArrayList<>();
            List<CrateItemPayload> itemRewards = new ArrayList<>();
            for (int sequence = 0; sequence < amount; sequence++) {
                CrateReward reward = select(definition.rewards());
                grant(transaction, playerId, reward, operationKey + ":reward:" + sequence, itemRewards);
                granted.add(new GrantedCrateReward(reward.id(), reward.type(), reward.rarity(), sequence, false));
            }
            Map<String, CrateReward> byId = new LinkedHashMap<>();
            definition.rewards().forEach(reward -> byId.put(reward.id(), reward));
            for (CrateDefinition.Milestone milestone : definition.milestones()) {
                if (beforeCount.count() < milestone.openCount() && afterCount >= milestone.openCount()) {
                    CrateReward reward = byId.get(milestone.rewardId());
                    grant(transaction, playerId, reward, operationKey + ":milestone:" + milestone.openCount(), itemRewards);
                    granted.add(new GrantedCrateReward(reward.id(), reward.type(), reward.rarity(), granted.size(), true));
                }
            }
            putCount(transaction, new CrateOpenCount(playerId, crateId, afterCount));
            if (!itemRewards.isEmpty()) DeliveryTransactionSupport.enqueue(transaction, MailboxDelivery.pending(
                    UUID.randomUUID(), playerId, operationKey, "magiccore/crate-items-v1",
                    JSON.writeValueAsBytes(itemRewards), clock.instant()));
            CrateOpening opening = new CrateOpening(UUID.randomUUID(), playerId, crateId, amount, granted, afterCount, clock.instant());
            openings.put(transaction, operationKey, opening, 0);
            return new CrateOpenResult(true, "OPENED", opening);
        }).thenApply(result -> {
            if (result.applied()) events.publish(new CrateOpened(playerId, crateId, amount, result.opening().id(), operationKey, clock.instant()));
            return result;
        });
    }

    @Override public CompletionStage<List<CrateOpening>> history(UUID playerId, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("history limit must be 1..100");
        return store.read(reader -> scanAll(openings,reader).stream().map(RecordRepository.KeyedVersionedValue::value)
                .filter(opening -> opening.playerId().equals(playerId))
                .sorted(Comparator.comparing(CrateOpening::openedAt).reversed()).limit(limit).toList());
    }

    private void grant(com.magicstudios.magiccore.storage.DataTransaction transaction, UUID playerId,
                       CrateReward reward, String operationKey, List<CrateItemPayload> items) throws Exception {
        switch (reward.type()) {
            case ITEM -> items.add(new CrateItemPayload(reward.material(), reward.amount(), reward.itemDataBase64()));
            case CURRENCY -> EconomyTransactionSupport.credit(transaction, playerId, requireCurrency(reward.currency()),
                    reward.amountMinor(), operationKey, "crate", "crate-reward:" + reward.id(), clock.instant());
            case KEY -> {
                CrateKeyBalance before = loadKey(transaction, playerId, reward.keyId());
                putKey(transaction, new CrateKeyBalance(playerId, reward.keyId(),
                        Math.addExact(before.amount(), reward.keyAmount()), clock.instant()));
            }
        }
    }

    private CrateReward select(List<CrateReward> rewards) {
        long total = rewards.stream().mapToLong(CrateReward::weight).reduce(0L, Math::addExact);
        long selected = randomBelow.applyAsLong(total);
        if (selected < 0 || selected >= total) throw new IllegalStateException("random source returned out-of-range value");
        for (CrateReward reward : rewards) {
            if (selected < reward.weight()) return reward;
            selected -= reward.weight();
        }
        throw new IllegalStateException("weighted selection failed");
    }

    private void validate(CrateDefinition definition) {
        if (!definition.id().matches("[A-Z][A-Z0-9_]*") || definition.maximumOpenAmount() < 1 || definition.rewards().isEmpty())
            throw new IllegalArgumentException("invalid crate " + definition.id());
        Map<String, CrateReward> ids = new LinkedHashMap<>();
        for (CrateReward reward : definition.rewards()) {
            if (ids.putIfAbsent(reward.id(), reward) != null) throw new IllegalArgumentException("duplicate crate reward " + reward.id());
            switch (reward.type()) {
                case ITEM -> { if (reward.amount() < 1) throw new IllegalArgumentException("item reward amount must be positive"); }
                case CURRENCY -> { requireCurrency(reward.currency()); if (reward.amountMinor() < 1) throw new IllegalArgumentException("currency reward must be positive"); }
                case KEY -> { if (reward.keyId() == null || reward.keyId().isBlank() || reward.keyAmount() < 1) throw new IllegalArgumentException("key reward is invalid"); }
            }
        }
        long prior = 0;
        for (var milestone : definition.milestones().stream().sorted(Comparator.comparingLong(CrateDefinition.Milestone::openCount)).toList()) {
            if (milestone.openCount() <= prior || !ids.containsKey(milestone.rewardId())) throw new IllegalArgumentException("invalid crate milestone");
            prior = milestone.openCount();
        }
    }

    private CrateDefinition requireDefinition(String id) {
        CrateDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("unknown crate " + id);
        return definition;
    }
    private CurrencyDefinition requireCurrency(String id) {
        CurrencyDefinition definition = currencies.get(id);
        if (definition == null) throw new IllegalArgumentException("unknown currency " + id);
        return definition;
    }
    private CrateKeyBalance loadKey(com.magicstudios.magiccore.storage.DataReader reader, UUID playerId, String keyId) throws Exception {
        return keys.get(reader, keyId + ":" + playerId).map(RecordRepository.VersionedValue::value)
                .orElse(new CrateKeyBalance(playerId, keyId, 0, clock.instant()));
    }
    private CrateKeyBalance putKey(com.magicstudios.magiccore.storage.DataTransaction transaction, CrateKeyBalance value) throws Exception {
        String key = value.keyId() + ":" + value.playerId();
        var current = keys.get(transaction, key);
        keys.put(transaction, key, value, current.map(RecordRepository.VersionedValue::revision).orElse(0L));
        return value;
    }
    private CrateOpenCount loadCount(com.magicstudios.magiccore.storage.DataReader reader, UUID playerId, String crateId) throws Exception {
        return counts.get(reader, crateId + ":" + playerId).map(RecordRepository.VersionedValue::value)
                .orElse(new CrateOpenCount(playerId, crateId, 0));
    }
    private void putCount(com.magicstudios.magiccore.storage.DataTransaction transaction, CrateOpenCount value) throws Exception {
        String key = value.crateId() + ":" + value.playerId();
        var current = counts.get(transaction, key);
        counts.put(transaction, key, value, current.map(RecordRepository.VersionedValue::revision).orElse(0L));
    }
    private static<T>List<RecordRepository.KeyedVersionedValue<T>>scanAll(RecordRepository<T>repository,
                                                                      com.magicstudios.magiccore.storage.DataReader reader)throws Exception{
        ArrayList<RecordRepository.KeyedVersionedValue<T>>all=new ArrayList<>();String after=null;
        while(true){var page=repository.scanPage(reader,after,1000);all.addAll(page);if(page.size()<1000)break;after=page.get(page.size()-1).key();}
        return all;
    }
}
