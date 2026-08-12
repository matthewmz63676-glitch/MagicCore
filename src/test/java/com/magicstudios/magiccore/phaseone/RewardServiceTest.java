package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.PersistentEconomyService;
import com.magicstudios.magiccore.modules.rewards.MilestonePolicy;
import com.magicstudios.magiccore.modules.rewards.PersistentRewardService;
import com.magicstudios.magiccore.modules.rewards.PlaytimeMilestone;
import com.magicstudios.magiccore.modules.rewards.RewardDefinition;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewardServiceTest {
    @Test
    void dailyClaimUsesStableIdDisplayAndCreditsExactlyOnce() {
        var store = store();
        try {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition coins = coins(1_000_000);
            RewardDefinition reward = new RewardDefinition("DAILY_COINS_COMMON", "<white>Coin Pouch</white>",
                    "COMMON", 1, "COINS", 10_000);
            var service = rewards(store, clock, coins, List.of(reward), List.of());
            var economy = new PersistentEconomyService(store, new DomainEventBus(), "COINS", Map.of("COINS", coins), clock);
            UUID player = UUID.randomUUID();

            var first = service.claimDaily(player, "daily:player:2026-08-10").toCompletableFuture().join();
            var replay = service.claimDaily(player, "daily:player:2026-08-10").toCompletableFuture().join();
            var tooSoon = service.claimDaily(player, "daily:player:second").toCompletableFuture().join();

            assertThat(first.applied()).isTrue();
            assertThat(first.claim().rewardId()).isEqualTo("DAILY_COINS_COMMON");
            assertThat(first.claim().rewardDisplay()).isEqualTo("<white>Coin Pouch</white>");
            assertThat(replay.applied()).isFalse();
            assertThat(replay.code()).isEqualTo("REPLAY");
            assertThat(tooSoon.code()).isEqualTo("NOT_READY");
            assertThat(economy.balance(player, "COINS").toCompletableFuture().join().minorUnits()).isEqualTo(10_000);
            assertThat(service.dailyState(player).toCompletableFuture().join().currentStreak()).isEqualTo(1);
        } finally {
            store.close();
        }
    }

    @Test
    void sequentialPlaytimeStateCannotSkipMilestonesOrTrustPresentationMetadata() {
        var store = store();
        try {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition coins = coins(1_000_000);
            List<PlaytimeMilestone> milestones = List.of(
                    new PlaytimeMilestone("PLAYTIME_60", "One Hour", 60, "COINS", 100),
                    new PlaytimeMilestone("PLAYTIME_300", "Five Hours", 300, "COINS", 500));
            var service = rewards(store, clock, coins,
                    List.of(new RewardDefinition("DAILY", "Daily", "COMMON", 1, "COINS", 1)), milestones);
            UUID player = UUID.randomUUID();

            assertThat(service.claimPlaytime(player, 300, "PLAYTIME_300", "skip")
                    .toCompletableFuture().join().code()).isEqualTo("PREVIOUS_MILESTONE_REQUIRED");
            assertThat(service.claimPlaytime(player, 59, "PLAYTIME_60", "metadata-cannot-authorize")
                    .toCompletableFuture().join().code()).isEqualTo("REQUIREMENT_NOT_MET");
            assertThat(service.claimPlaytime(player, 60, "PLAYTIME_60", "first")
                    .toCompletableFuture().join().applied()).isTrue();
            assertThat(service.claimPlaytime(player, 300, "PLAYTIME_300", "second")
                    .toCompletableFuture().join().applied()).isTrue();
            assertThat(service.playtimeState(player).toCompletableFuture().join().claimedMilestones())
                    .containsExactlyInAnyOrder("PLAYTIME_60", "PLAYTIME_300");
        } finally {
            store.close();
        }
    }

    @Test
    void failedEconomyCreditRollsBackClaimAndIdempotencyReservation() {
        var store = store();
        try {
            MutableClock clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition constrained = coins(100);
            UUID player = UUID.randomUUID();
            var failing = rewards(store, clock, constrained,
                    List.of(new RewardDefinition("TOO_LARGE", "Too Large", "RARE", 1, "COINS", 101)), List.of());

            assertThatThrownBy(() -> failing.claimDaily(player, "same-operation").toCompletableFuture().join())
                    .isInstanceOf(CompletionException.class).hasRootCauseMessage("MAXIMUM_BALANCE_EXCEEDED");
            assertThat(failing.dailyState(player).toCompletableFuture().join().lastClaimAt()).isNull();

            var safe = rewards(store, clock, constrained,
                    List.of(new RewardDefinition("SAFE", "Safe", "COMMON", 1, "COINS", 100)), List.of());
            assertThat(safe.claimDaily(player, "same-operation").toCompletableFuture().join().applied()).isTrue();
        } finally {
            store.close();
        }
    }

    private static PersistentRewardService rewards(InMemoryTransactionalDataStore store, MutableClock clock,
                                                    CurrencyDefinition coins, List<RewardDefinition> daily,
                                                    List<PlaytimeMilestone> milestones) {
        return new PersistentRewardService(store, new DomainEventBus(), Map.of("COINS", coins), daily,
                Duration.ofHours(24), milestones, MilestonePolicy.SEQUENTIAL, clock, bound -> 0);
    }

    private static CurrencyDefinition coins(long max) {
        return new CurrencyDefinition("COINS", "Coins", "$", 2, 0, max);
    }

    private static InMemoryTransactionalDataStore store() {
        return new InMemoryTransactionalDataStore(new BoundedIoExecutor(3, 128, "rewards-test"));
    }
}
