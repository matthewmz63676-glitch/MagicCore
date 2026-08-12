package com.magicstudios.magiccore.phasethree;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.auction.AuctionService;
import com.magicstudios.magiccore.modules.auction.PersistentAuctionService;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionServiceTest {
    @Test
    void purchaseAtomicallyPaysSellerClosesListingAndQueuesBuyerItemOnce() {
        var store = store();
        try {
            var clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 1_000, 1_000_000);
            AtomicInteger removals = new AtomicInteger();
            InventoryRemovalPort inventory = (player, fingerprint, quantity, operation) -> {
                removals.incrementAndGet();
                return CompletableFuture.completedFuture(new InventoryRemovalPort.RemovalReceipt(true, "REMOVED",
                        Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));
            };
            var auctions = new PersistentAuctionService(store, limits(3), inventory, coins, new DomainEventBus(), clock,
                    Duration.ofMinutes(5), Duration.ofDays(7), 100, 100_000, 25, java.util.Set.of("weapons"));
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID seller = UUID.randomUUID(); UUID buyer = UUID.randomUUID();
            ItemFingerprint item = ItemFingerprint.of("DIAMOND_SWORD", new byte[]{4, 5, 6});

            var listing = auctions.create(seller, "weapons", item, 1, 300, Duration.ofHours(1), "create-1")
                    .toCompletableFuture().join().listing();
            assertThat(removals).hasValue(1);
            assertThat(auctions.purchase(buyer, listing.id(), "purchase-1").toCompletableFuture().join().applied()).isTrue();
            assertThat(auctions.purchase(buyer, listing.id(), "purchase-1").toCompletableFuture().join().applied()).isFalse();
            assertThat(mailbox.pending(buyer, 10).toCompletableFuture().join()).hasSize(1);
            assertThat(balance(store, seller, coins)).isEqualTo(1_275);
            assertThat(balance(store, buyer, coins)).isEqualTo(700);
            assertThat(auctions.history(seller, 10).toCompletableFuture().join()).singleElement()
                    .extracting(value -> value.status().name()).isEqualTo("SOLD");
        } finally { store.close(); }
    }

    @Test
    void expiredListingReturnsEscrowAndCannotBePurchased() {
        var store = store();
        try {
            var clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 1_000, 1_000_000);
            InventoryRemovalPort inventory = (player, fingerprint, quantity, operation) -> CompletableFuture.completedFuture(
                    new InventoryRemovalPort.RemovalReceipt(true, "REMOVED", Base64.getEncoder().encodeToString(new byte[]{7})));
            var auctions = new PersistentAuctionService(store, limits(1), inventory, coins, new DomainEventBus(), clock,
                    Duration.ofMinutes(5), Duration.ofDays(7), 100, 100_000, 0, java.util.Set.of("blocks"));
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID seller = UUID.randomUUID();
            var listing = auctions.create(seller, "blocks", ItemFingerprint.of("STONE", new byte[]{1}), 16,
                    200, Duration.ofMinutes(5), "create-expiring").toCompletableFuture().join().listing();
            clock.advance(Duration.ofMinutes(6));
            assertThat(auctions.expire("expire-run-1", 10).toCompletableFuture().join()).isEqualTo(1);
            assertThat(mailbox.pending(seller, 10).toCompletableFuture().join()).hasSize(1);
            assertThatThrownBy(() -> auctions.purchase(UUID.randomUUID(), listing.id(), "late-buy").toCompletableFuture().join())
                    .hasRootCauseMessage("LISTING_UNAVAILABLE");
            assertThat(auctions.search("stone", "blocks", AuctionService.Sort.NEWEST, 0, 10)
                    .toCompletableFuture().join().total()).isZero();
        } finally { store.close(); }
    }

    private static long balance(InMemoryTransactionalDataStore store, UUID player, CurrencyDefinition currency) {
        return store.read(reader -> EconomyTransactionSupport.balance(reader, player, currency).minorUnits())
                .toCompletableFuture().join();
    }
    private static CapabilityService limits(int limit) {
        return new CapabilityService() {
            @Override public java.util.concurrent.CompletionStage<Boolean> has(UUID id, String capability) { return CompletableFuture.completedFuture(true); }
            @Override public java.util.concurrent.CompletionStage<Integer> limit(UUID id, String name) { return CompletableFuture.completedFuture(limit); }
            @Override public java.util.concurrent.CompletionStage<Boolean> canTarget(UUID actor, UUID target) { return CompletableFuture.completedFuture(true); }
        };
    }
    private static InMemoryTransactionalDataStore store() {
        return new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "auction-test"));
    }
    private static final class MutableClock extends Clock {
        private Instant instant; private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
