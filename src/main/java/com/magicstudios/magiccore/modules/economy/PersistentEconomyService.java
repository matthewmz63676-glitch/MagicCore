package com.magicstudios.magiccore.modules.economy;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentEconomyService implements EconomyService {
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final String primaryCurrency;
    private final Map<String, CurrencyDefinition> currencies;
    private final Clock clock;
    private final RecordRepository<EconomyTransaction> ledger =
            new RecordRepository<>("economy.ledger", EconomyTransaction.class);

    public PersistentEconomyService(TransactionalDataStore store, DomainEventBus events,
                                    String primaryCurrency, Map<String, CurrencyDefinition> currencies, Clock clock) {
        this.store = store;
        this.events = events;
        this.primaryCurrency = primaryCurrency;
        this.currencies = Map.copyOf(currencies);
        this.clock = clock;
        definition(primaryCurrency);
    }

    @Override
    public String primaryCurrency() {
        return primaryCurrency;
    }

    @Override
    public Map<String, CurrencyDefinition> currencies() {
        return currencies;
    }

    @Override
    public CompletionStage<Money> balance(UUID playerId, String currency) {
        CurrencyDefinition definition = definition(currency);
        return store.read(reader -> new Money(currency,
                EconomyTransactionSupport.balance(reader, playerId, definition).minorUnits()));
    }

    @Override
    public CompletionStage<EconomyMutation> transfer(UUID from, UUID to, Money amount, String operationKey) {
        CurrencyDefinition definition = definition(amount.currency());
        return store.transact("economy-transfer:" + operationKey, transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "economy", operationKey)) {
                Money balance = new Money(definition.id(), EconomyTransactionSupport.balance(transaction, from, definition).minorUnits());
                return new EconomyMutation(false, balance, null);
            }
            EconomyTransaction ledger = EconomyTransactionSupport.transfer(transaction, from, to, definition,
                    amount.minorUnits(), operationKey, clock.instant());
            return new EconomyMutation(true, new Money(definition.id(), ledger.fromAfter()), ledger);
        }).thenApply(result -> {
            if (result.applied()) {
                EconomyTransaction entry = result.transaction();
                events.publish(new BalanceChanged(from, entry.currency(), entry.fromBefore(), entry.fromAfter(),
                        operationKey, entry.timestamp()));
                events.publish(new BalanceChanged(to, entry.currency(), entry.toBefore(), entry.toAfter(),
                        operationKey, entry.timestamp()));
            }
            return result;
        });
    }

    @Override
    public CompletionStage<EconomyMutation> adjust(UUID playerId, Money delta, String actor, String reason, String operationKey) {
        CurrencyDefinition definition = definition(delta.currency());
        return store.transact("economy-adjust:" + operationKey, transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "economy", operationKey)) {
                Money current = new Money(definition.id(), EconomyTransactionSupport.balance(transaction, playerId, definition).minorUnits());
                return new EconomyMutation(false, current, null);
            }
            var applied = EconomyTransactionSupport.credit(transaction, playerId, definition, delta.minorUnits(),
                    operationKey, actor, reason, clock.instant());
            return new EconomyMutation(true, new Money(definition.id(), applied.afterMinor()), applied.transaction());
        }).thenApply(result -> {
            if (result.applied()) {
                EconomyTransaction entry = result.transaction();
                long before = entry.amountMinor() == 0 ? result.resultingBalance().minorUnits()
                        : result.resultingBalance().minorUnits() - delta.minorUnits();
                events.publish(new BalanceChanged(playerId, delta.currency(), before,
                        result.resultingBalance().minorUnits(), operationKey, entry.timestamp()));
            }
            return result;
        });
    }

    @Override
    public CompletionStage<List<EconomyTransaction>> transactions(String afterKey, int limit) {
        return store.read(reader -> ledger.scan(reader, afterKey, limit).stream()
                .map(RecordRepository.VersionedValue::value).toList());
    }

    private CurrencyDefinition definition(String id) {
        CurrencyDefinition definition = currencies.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown currency: " + id);
        return definition;
    }
}
