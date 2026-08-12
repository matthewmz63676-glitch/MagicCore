package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.crates.CrateCost;
import com.magicstudios.magiccore.modules.crates.CrateDefinition;
import com.magicstudios.magiccore.modules.crates.CrateReward;
import com.magicstudios.magiccore.modules.crates.PersistentCrateService;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.PersistentEconomyService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrateServiceTest {
    private static final CurrencyDefinition COINS = new CurrencyDefinition("COINS", "Coins", "$", 0, 1_000, 1_000_000);

    @Test void keyOpeningIsAtomicMilestonedAndReplaySafe() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "crate-key-test"));
        try {
            var clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
            var item = new CrateReward("DIAMONDS", CrateReward.Type.ITEM, 1, "COMMON", "DIAMOND", 2, "", "", 0, "", 0);
            var bonus = new CrateReward("BONUS_KEY", CrateReward.Type.KEY, 1, "EPIC", "AIR", 0, "", "", 0, "VOTE_KEY", 1);
            var crate = new CrateDefinition("VOTE", "Vote", new CrateCost(CrateCost.Type.KEY, "VOTE_KEY", 1), 10,
                    List.of(item, bonus), List.of(new CrateDefinition.Milestone(2, "BONUS_KEY")));
            var service = new PersistentCrateService(store, new DomainEventBus(), clock, Map.of("VOTE", crate),
                    Map.of("COINS", COINS), "COINS", ignored -> 0);
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID player = UUID.randomUUID();

            service.grantKeys(player, "VOTE_KEY", 2, "keys-1").toCompletableFuture().join();
            var opened = service.open(player, "VOTE", 2, "open-1").toCompletableFuture().join();
            assertThat(opened.applied()).isTrue();
            assertThat(opened.opening().rewards()).hasSize(3);
            assertThat(opened.opening().rewards()).filteredOn(reward -> reward.milestone()).singleElement()
                    .extracting(reward -> reward.rewardId()).isEqualTo("BONUS_KEY");
            assertThat(service.keyBalance(player, "VOTE_KEY").toCompletableFuture().join().amount()).isEqualTo(1);
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).hasSize(1);

            var replay = service.open(player, "VOTE", 2, "open-1").toCompletableFuture().join();
            assertThat(replay.applied()).isFalse();
            assertThat(replay.opening().id()).isEqualTo(opened.opening().id());
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).hasSize(1);
            assertThat(service.keyBalance(player, "VOTE_KEY").toCompletableFuture().join().amount()).isEqualTo(1);
        } finally { store.close(); }
    }

    @Test void currencyCostAndRewardCommitInTheSameTransaction() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "crate-money-test"));
        try {
            var clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
            var reward = new CrateReward("REFUND", CrateReward.Type.CURRENCY, 1, "COMMON", "AIR", 0, "", "COINS", 50, "", 0);
            var crate = new CrateDefinition("COIN", "Coin", new CrateCost(CrateCost.Type.CURRENCY, "", 100), 5,
                    List.of(reward), List.of());
            var events = new DomainEventBus();
            var service = new PersistentCrateService(store, events, clock, Map.of("COIN", crate),
                    Map.of("COINS", COINS), "COINS", ignored -> 0);
            var economy = new PersistentEconomyService(store, events, "COINS", Map.of("COINS", COINS), clock);
            UUID player = UUID.randomUUID();

            service.open(player, "COIN", 2, "coin-open").toCompletableFuture().join();
            assertThat(economy.balance(player, "COINS").toCompletableFuture().join().minorUnits()).isEqualTo(900);
        } finally { store.close(); }
    }
}
