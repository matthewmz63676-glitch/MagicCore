package com.magicstudios.magiccore.phasetwo;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.essentials.TeleportRequest;
import com.magicstudios.magiccore.modules.essentials.TeleportRequestService;
import com.magicstudios.magiccore.modules.essentials.TeleportWarmupService;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.modules.essentials.RtpBounds;
import com.magicstudios.magiccore.modules.essentials.RtpCandidatePlanner;
import com.magicstudios.magiccore.modules.essentials.PersistentTeleportPolicyService;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.kits.KitDefinition;
import com.magicstudios.magiccore.modules.kits.PersistentKitService;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.settings.PersistentPlayerSettingsService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.shop.InternalShopService;
import com.magicstudios.magiccore.modules.shop.ShopProduct;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.modules.shop.PersistentSellService;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.playerwarps.PersistentPlayerWarpService;
import com.magicstudios.magiccore.protection.AllowAllProtectionService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhaseTwoServicesTest {
    @Test
    void teleportPolicyRefundsFailuresAndStartsCooldownOnlyAfterSuccess() {
        var store = store();
        try {
            var clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 500, 100_000);
            var policy = new PersistentTeleportPolicyService(store, coins, 100, Duration.ofSeconds(10), clock);
            UUID player = UUID.randomUUID();
            var failed = policy.reserve(player, "tp-failed").toCompletableFuture().join();
            assertThat(policy.refund(failed, "platform").toCompletableFuture().join()).isTrue();
            assertThat(store.read(reader -> EconomyTransactionSupport.balance(reader, player, coins).minorUnits())
                    .toCompletableFuture().join()).isEqualTo(500);

            var successful = policy.reserve(player, "tp-success").toCompletableFuture().join();
            assertThat(policy.complete(successful).toCompletableFuture().join()).isTrue();
            assertThat(store.read(reader -> EconomyTransactionSupport.balance(reader, player, coins).minorUnits())
                    .toCompletableFuture().join()).isEqualTo(400);
            assertThatThrownBy(() -> policy.reserve(player, "tp-too-soon").toCompletableFuture().join())
                    .hasRootCauseMessage("TELEPORT_COOLDOWN_UNTIL:2026-08-10T00:00:10Z");
            clock.advance(Duration.ofSeconds(10));
            assertThat(policy.reserve(player, "tp-after-cooldown").toCompletableFuture().join()).isNotNull();
        } finally { store.close(); }
    }

    @Test
    void playerWarpCreationHonorsRankLimitAndVisitIsIdempotent() {
        var store = store();
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("UTC"));
            CapabilityService oneWarp = new CapabilityService() {
                @Override public java.util.concurrent.CompletionStage<Boolean> has(UUID id, String c) { return CompletableFuture.completedFuture(true); }
                @Override public java.util.concurrent.CompletionStage<Integer> limit(UUID id, String l) { return CompletableFuture.completedFuture(1); }
                @Override public java.util.concurrent.CompletionStage<Boolean> canTarget(UUID a, UUID t) { return CompletableFuture.completedFuture(true); }
            };
            var service = new PersistentPlayerWarpService(store, oneWarp, new AllowAllProtectionService(), clock, 24);
            UUID owner = UUID.randomUUID();
            var position = new WorldPosition(UUID.randomUUID(), "world", 10, 70, 20, 0, 0);
            assertThat(service.create(owner, "Farm", "builds", position, "create-1").toCompletableFuture().join().applied()).isTrue();
            assertThatThrownBy(() -> service.create(owner, "Mine", "builds", position, "create-2").toCompletableFuture().join())
                    .hasRootCauseMessage("PLAYER_WARP_LIMIT_REACHED");
            assertThat(service.recordVisit("farm", UUID.randomUUID(), "visit-1").toCompletableFuture().join()).isTrue();
            assertThat(service.recordVisit("farm", UUID.randomUUID(), "visit-1").toCompletableFuture().join()).isFalse();
            assertThat(service.findActive("farm").toCompletableFuture().join()).get().extracting(w -> w.visits()).isEqualTo(1L);
        } finally { store.close(); }
    }

    @Test
    void sellReservesBeforeRemovalAndCreditsExactQuoteOnlyOnce() {
        var store = store();
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("UTC"));
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 500, 100_000);
            ShopProduct stone = new ShopProduct("stone", "blocks", "STONE", 16, 200, 50, "");
            AtomicInteger removals = new AtomicInteger();
            InventoryRemovalPort inventory = (player, fingerprint, quantity, operation) -> {
                removals.incrementAndGet();
                return CompletableFuture.completedFuture(new InventoryRemovalPort.RemovalReceipt(true, "REMOVED", "recovery"));
            };
            var selling = new PersistentSellService(store, inventory, coins, clock, Duration.ofSeconds(30), List.of(stone));
            UUID player = UUID.randomUUID();
            ItemFingerprint fingerprint = ItemFingerprint.of("STONE", new byte[]{1, 2, 3});
            var quote = selling.quote(player, "stone", fingerprint, 32, "quote-1").toCompletableFuture().join();
            var sold = selling.execute(player, quote.id(), "execute-1").toCompletableFuture().join();
            var replay = selling.execute(player, quote.id(), "execute-1").toCompletableFuture().join();
            assertThat(sold).extracting(r -> r.applied(), r -> r.creditedMinor(), r -> r.balanceAfterMinor())
                    .containsExactly(true, 100L, 600L);
            assertThat(replay.applied()).isFalse();
            assertThat(removals).hasValue(1);
        } finally { store.close(); }
    }

    @Test
    void teleportWarmupCancelsOnMovementAndCanOnlyCompleteOnce() {
        var clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
        var warmups = new TeleportWarmupService(clock, 0.25);
        UUID player = UUID.randomUUID();
        UUID world = UUID.randomUUID();
        var origin = new WorldPosition(world, "world", 0, 64, 0, 0, 0);
        var destination = new WorldPosition(world, "world", 100, 70, 100, 0, 0);
        warmups.begin(player, origin, destination, Duration.ofSeconds(3), "tp-1");
        assertThat(warmups.observeMovement(player, new WorldPosition(world, "world", .3, 64, 0, 0, 0))).isTrue();
        assertThat(warmups.takeReady(player)).isEmpty();

        warmups.begin(player, origin, destination, Duration.ofSeconds(3), "tp-2");
        clock.advance(Duration.ofSeconds(3));
        assertThat(warmups.takeReady(player)).isPresent();
        assertThat(warmups.takeReady(player)).isEmpty();
    }

    @Test
    void rtpCandidateGenerationIsBoundedByRadiusAndAttemptCount() {
        var bounds = new RtpBounds(10, -10, 100, 500, 17);
        var candidates = new RtpCandidatePlanner().plan(bounds, new Random(42));
        assertThat(candidates).hasSize(17).allSatisfy(candidate -> {
            double radius = Math.hypot(candidate.x() - 10, candidate.z() + 10);
            assertThat(radius).isBetween(100.0, 500.0);
        });
    }

    @Test
    void settingBlocksTeleportRequestsAndExpiredRequestsCannotBeAccepted() {
        var store = store();
        try {
            var clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            var settings = new PersistentPlayerSettingsService(store, clock);
            var requests = new TeleportRequestService(settings, clock, Duration.ofSeconds(30));
            UUID requester = UUID.randomUUID();
            UUID target = UUID.randomUUID();

            requests.request(requester, target, TeleportRequest.Direction.REQUESTER_TO_TARGET).toCompletableFuture().join();
            clock.advance(Duration.ofSeconds(31));
            assertThat(requests.accept(target)).isEmpty();

            settings.set(target, PlayerSetting.TELEPORT_REQUESTS, false, "setting-1").toCompletableFuture().join();
            assertThatThrownBy(() -> requests.request(requester, target,
                    TeleportRequest.Direction.REQUESTER_TO_TARGET).toCompletableFuture().join())
                    .hasRootCauseMessage("REQUESTS_DISABLED");
        } finally { store.close(); }
    }

    @Test
    void kitClaimAndMailboxDeliveryCommitOnceWithCooldown() {
        var store = store();
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("UTC"));
            KitDefinition starter = new KitDefinition("starter", "Starter", Duration.ofHours(24), "",
                    List.of(new KitDefinition.KitItem("STONE", 16, "")));
            var kits = new PersistentKitService(store, allowAll(), clock, List.of(starter));
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID player = UUID.randomUUID();

            assertThat(kits.claim(player, "starter", "kit-op").toCompletableFuture().join().applied()).isTrue();
            assertThat(kits.claim(player, "starter", "kit-op").toCompletableFuture().join().code()).isEqualTo("REPLAY");
            assertThat(kits.claim(player, "starter", "kit-op-2").toCompletableFuture().join().code()).isEqualTo("COOLDOWN");
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).hasSize(1);
        } finally { store.close(); }
    }

    @Test
    void shopChargeAndDeliveryAreAtomicAndDuplicateClickIsIdempotent() {
        var store = store();
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("UTC"));
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 500, 100_000);
            var shop = new InternalShopService(store, coins, clock,
                    List.of(new ShopProduct("stone", "blocks", "STONE", 16, 200, 50, "")));
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID player = UUID.randomUUID();

            var first = shop.buy(player, "stone", 2, "click-1").toCompletableFuture().join();
            var replay = shop.buy(player, "stone", 2, "click-1").toCompletableFuture().join();
            assertThat(first.balanceAfterMinor()).isEqualTo(100);
            assertThat(replay.applied()).isFalse();
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).hasSize(1);

            assertThatThrownBy(() -> shop.buy(player, "stone", 1, "click-2").toCompletableFuture().join())
                    .hasRootCauseMessage("INSUFFICIENT_FUNDS");
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).hasSize(1);
        } finally { store.close(); }
    }

    private static CapabilityService allowAll() {
        return new CapabilityService() {
            @Override public java.util.concurrent.CompletionStage<Boolean> has(UUID id, String capability) { return CompletableFuture.completedFuture(true); }
            @Override public java.util.concurrent.CompletionStage<Integer> limit(UUID id, String limit) { return CompletableFuture.completedFuture(100); }
            @Override public java.util.concurrent.CompletionStage<Boolean> canTarget(UUID actor, UUID target) { return CompletableFuture.completedFuture(true); }
        };
    }

    private static InMemoryTransactionalDataStore store() {
        return new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "phase-two-test"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
