package com.magicstudios.magiccore.modules.rewards;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.IntUnaryOperator;

public final class PersistentRewardService implements RewardService {
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final Map<String, CurrencyDefinition> currencies;
    private final List<RewardDefinition> dailyPool;
    private final Duration dailyCooldown;
    private final List<PlaytimeMilestone> milestones;
    private final MilestonePolicy milestonePolicy;
    private final Clock clock;
    private final IntUnaryOperator randomBounded;
    private final RecordRepository<DailyRewardState> dailyStates =
            new RecordRepository<>("rewards.daily-state", DailyRewardState.class);
    private final RecordRepository<PlaytimeRewardState> playtimeStates =
            new RecordRepository<>("rewards.playtime-state", PlaytimeRewardState.class);
    private final RecordRepository<RewardClaim> claims =
            new RecordRepository<>("rewards.claim", RewardClaim.class);

    public PersistentRewardService(TransactionalDataStore store, DomainEventBus events,
                                   Map<String, CurrencyDefinition> currencies,
                                   List<RewardDefinition> dailyPool, Duration dailyCooldown,
                                   List<PlaytimeMilestone> milestones, MilestonePolicy milestonePolicy,
                                   Clock clock, IntUnaryOperator randomBounded) {
        this.store = store;
        this.events = events;
        this.currencies = Map.copyOf(currencies);
        this.dailyPool = List.copyOf(dailyPool);
        this.dailyCooldown = dailyCooldown;
        this.milestones = milestones.stream().sorted(java.util.Comparator.comparingLong(PlaytimeMilestone::requiredMinutes)).toList();
        this.milestonePolicy = milestonePolicy;
        this.clock = clock;
        this.randomBounded = randomBounded;
        if (this.dailyPool.isEmpty()) throw new IllegalArgumentException("Daily reward pool cannot be empty");
        this.dailyPool.forEach(reward -> currency(reward.currency()));
        this.milestones.forEach(reward -> currency(reward.currency()));
    }

    public static IntUnaryOperator defaultRandom() {
        Random random = new Random();
        return random::nextInt;
    }

    @Override
    public List<RewardDefinition> dailyPool() {
        return dailyPool;
    }

    @Override
    public List<PlaytimeMilestone> milestones() {
        return milestones;
    }

    @Override
    public CompletionStage<RewardClaimResult> claimDaily(UUID playerId, String operationKey) {
        Instant now = clock.instant();
        return store.transact("reward-daily:" + operationKey, transaction -> {
            var replay = claims.get(transaction, operationKey);
            if (replay.isPresent()) return RewardClaimResult.claimed(false, replay.get().value());
            var current = dailyStates.get(transaction, playerId.toString());
            DailyRewardState state = current.map(RecordRepository.VersionedValue::value)
                    .orElse(new DailyRewardState(playerId, null, 0, 0));
            if (state.lastClaimAt() != null) {
                Instant readyAt = state.lastClaimAt().plus(dailyCooldown);
                if (now.isBefore(readyAt)) return RewardClaimResult.unavailable("NOT_READY", Duration.between(now, readyAt));
            }
            if (!IdempotencyKeys.reserve(transaction, "reward", operationKey)) {
                throw new IllegalStateException("Reward idempotency record exists without claim record");
            }
            RewardDefinition reward = chooseDaily();
            CurrencyDefinition definition = currency(reward.currency());
            EconomyTransactionSupport.credit(transaction, playerId, definition, reward.amountMinor(),
                    operationKey, "reward-service", "daily:" + reward.id(), now);
            int streak = state.lastClaimAt() != null && !now.isAfter(state.lastClaimAt().plus(dailyCooldown.multipliedBy(2)))
                    ? state.currentStreak() + 1 : 1;
            DailyRewardState updatedState = new DailyRewardState(playerId, now, streak, Math.max(streak, state.bestStreak()));
            dailyStates.put(transaction, playerId.toString(), updatedState,
                    current.map(RecordRepository.VersionedValue::revision).orElse(0L));
            RewardClaim claim = new RewardClaim(UUID.randomUUID(), operationKey, playerId, "DAILY", reward.id(),
                    reward.display(), reward.currency(), reward.amountMinor(), now);
            claims.put(transaction, operationKey, claim, 0);
            return RewardClaimResult.claimed(true, claim);
        }).thenApply(this::publish);
    }

    @Override
    public CompletionStage<RewardClaimResult> claimPlaytime(UUID playerId, long authoritativePlaytimeMinutes,
                                                            String milestoneId, String operationKey) {
        PlaytimeMilestone milestone = milestones.stream().filter(item -> item.id().equals(milestoneId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown playtime milestone: " + milestoneId));
        Instant now = clock.instant();
        return store.transact("reward-playtime:" + operationKey, transaction -> {
            var replay = claims.get(transaction, operationKey);
            if (replay.isPresent()) return RewardClaimResult.claimed(false, replay.get().value());
            var current = playtimeStates.get(transaction, playerId.toString());
            PlaytimeRewardState state = current.map(RecordRepository.VersionedValue::value)
                    .orElse(new PlaytimeRewardState(playerId, Set.of()));
            if (state.claimedMilestones().contains(milestoneId)) return RewardClaimResult.unavailable("ALREADY_CLAIMED", Duration.ZERO);
            if (authoritativePlaytimeMinutes < milestone.requiredMinutes()) return RewardClaimResult.unavailable("REQUIREMENT_NOT_MET", Duration.ZERO);
            if (milestonePolicy == MilestonePolicy.SEQUENTIAL) {
                for (PlaytimeMilestone previous : milestones) {
                    if (previous.id().equals(milestoneId)) break;
                    if (!state.claimedMilestones().contains(previous.id())) {
                        return RewardClaimResult.unavailable("PREVIOUS_MILESTONE_REQUIRED", Duration.ZERO);
                    }
                }
            }
            if (!IdempotencyKeys.reserve(transaction, "reward", operationKey)) {
                throw new IllegalStateException("Reward idempotency record exists without claim record");
            }
            EconomyTransactionSupport.credit(transaction, playerId, currency(milestone.currency()), milestone.amountMinor(),
                    operationKey, "reward-service", "playtime:" + milestone.id(), now);
            Set<String> claimed = new LinkedHashSet<>(state.claimedMilestones());
            claimed.add(milestone.id());
            playtimeStates.put(transaction, playerId.toString(), new PlaytimeRewardState(playerId, claimed),
                    current.map(RecordRepository.VersionedValue::revision).orElse(0L));
            RewardClaim claim = new RewardClaim(UUID.randomUUID(), operationKey, playerId, "PLAYTIME", milestone.id(),
                    milestone.display(), milestone.currency(), milestone.amountMinor(), now);
            claims.put(transaction, operationKey, claim, 0);
            return RewardClaimResult.claimed(true, claim);
        }).thenApply(this::publish);
    }

    @Override
    public CompletionStage<DailyRewardState> dailyState(UUID playerId) {
        return store.read(reader -> dailyStates.get(reader, playerId.toString())
                .map(RecordRepository.VersionedValue::value).orElse(new DailyRewardState(playerId, null, 0, 0)));
    }

    @Override
    public CompletionStage<PlaytimeRewardState> playtimeState(UUID playerId) {
        return store.read(reader -> playtimeStates.get(reader, playerId.toString())
                .map(RecordRepository.VersionedValue::value).orElse(new PlaytimeRewardState(playerId, Set.of())));
    }

    private RewardDefinition chooseDaily() {
        int total = dailyPool.stream().mapToInt(RewardDefinition::weight).sum();
        int selected = randomBounded.applyAsInt(total);
        if (selected < 0 || selected >= total) throw new IllegalStateException("Random source returned out-of-range value");
        for (RewardDefinition reward : dailyPool) {
            selected -= reward.weight();
            if (selected < 0) return reward;
        }
        throw new IllegalStateException("Reward selection failed");
    }

    private CurrencyDefinition currency(String id) {
        CurrencyDefinition definition = currencies.get(id);
        if (definition == null) throw new IllegalArgumentException("Reward references unknown currency " + id);
        return definition;
    }

    private RewardClaimResult publish(RewardClaimResult result) {
        if (result.applied()) {
            RewardClaim claim = result.claim();
            events.publish(new RewardClaimed(claim.playerId(), claim.rewardId(), claim.claimType(),
                    claim.operationKey(), claim.claimedAt()));
        }
        return result;
    }
}
