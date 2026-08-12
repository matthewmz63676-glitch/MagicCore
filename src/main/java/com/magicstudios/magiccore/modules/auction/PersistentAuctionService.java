package com.magicstudios.magiccore.modules.auction;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentAuctionService implements AuctionService {
    private final TransactionalDataStore store;
    private final CapabilityService capabilities;
    private final InventoryRemovalPort inventory;
    private final CurrencyDefinition currency;
    private final DomainEventBus events;
    private final Clock clock;
    private final Duration minimumDuration;
    private final Duration maximumDuration;
    private final long minimumPriceMinor;
    private final long maximumPriceMinor;
    private final long listingFeeMinor;
    private final Set<String> categories;
    private final RecordRepository<AuctionListing> listings = new RecordRepository<>("auction.listing", AuctionListing.class);

    public PersistentAuctionService(TransactionalDataStore store, CapabilityService capabilities,
                                    InventoryRemovalPort inventory, CurrencyDefinition currency,
                                    DomainEventBus events, Clock clock, Duration minimumDuration,
                                    Duration maximumDuration, long minimumPriceMinor,
                                    long maximumPriceMinor, long listingFeeMinor, Set<String> categories) {
        this.store = store; this.capabilities = capabilities; this.inventory = inventory; this.currency = currency;
        this.events = events; this.clock = clock; this.minimumDuration = minimumDuration; this.maximumDuration = maximumDuration;
        this.minimumPriceMinor = minimumPriceMinor; this.maximumPriceMinor = maximumPriceMinor; this.listingFeeMinor = listingFeeMinor;
        this.categories = categories.stream().map(PersistentAuctionService::normalize).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public CompletionStage<AuctionMutation> create(UUID sellerId, String category, ItemFingerprint fingerprint,
                                                    int quantity, long priceMinor, Duration duration, String operationKey) {
        validateCreate(category, quantity, priceMinor, duration);
        return capabilities.limit(sellerId, "AUCTION_SLOTS").thenCompose(limit -> prepare(sellerId, category,
                fingerprint, quantity, priceMinor, duration, operationKey, limit)).thenCompose(prepared -> {
            if (!prepared.applied()) return CompletableFuture.completedFuture(prepared);
            return inventory.removeExact(sellerId, fingerprint, quantity, operationKey + ":remove")
                    .handle((receipt, failure) -> new RemovalOutcome(receipt, failure))
                    .thenCompose(outcome -> {
                        if (outcome.failure() != null) return reject(prepared.listing(), "REMOVAL_FAILURE", operationKey)
                                .thenCompose(ignored -> CompletableFuture.failedFuture(outcome.failure()));
                        return outcome.receipt().removed()
                                ? activate(prepared.listing(), outcome.receipt().recoveryPayloadBase64(), operationKey)
                                : reject(prepared.listing(), outcome.receipt().code(), operationKey);
                    });
        });
    }

    private CompletionStage<AuctionMutation> prepare(UUID sellerId, String category, ItemFingerprint fingerprint,
                                                      int quantity, long priceMinor, Duration duration,
                                                      String operationKey, int limit) {
        return store.transact("auction-prepare:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "auction-create", operationKey))
                return new AuctionMutation(false, "REPLAY", null);
            long active = listings.scan(tx, null, 1000).stream().map(RecordRepository.VersionedValue::value)
                    .filter(listing -> listing.sellerId().equals(sellerId))
                    .filter(listing -> listing.status() == AuctionListing.Status.PREPARING || listing.status() == AuctionListing.Status.ACTIVE).count();
            if (active >= limit) throw new IllegalStateException("AUCTION_SLOT_LIMIT_REACHED");
            if (listingFeeMinor > 0) EconomyTransactionSupport.credit(tx, sellerId, currency, -listingFeeMinor,
                    operationKey + ":fee", sellerId.toString(), "auction-listing-fee", clock.instant());
            var now = clock.instant();
            AuctionListing listing = new AuctionListing(UUID.randomUUID(), sellerId, null, normalize(category),
                    fingerprint, quantity, "", currency.id(), priceMinor, listingFeeMinor,
                    AuctionListing.Status.PREPARING, now, null, now.plus(duration), null, null);
            listings.put(tx, listing.id().toString(), listing, 0);
            return new AuctionMutation(true, "PREPARED", listing);
        });
    }

    private CompletionStage<AuctionMutation> activate(AuctionListing listing, String payload, String operationKey) {
        return store.transact("auction-activate:" + operationKey, tx -> {
            var current = listings.get(tx, listing.id().toString()).orElseThrow();
            if (current.value().status() != AuctionListing.Status.PREPARING) return new AuctionMutation(false, "STATE_CONFLICT", current.value());
            AuctionListing active = copy(current.value(), AuctionListing.Status.ACTIVE, null, payload, clock.instant(), null, null);
            listings.put(tx, listing.id().toString(), active, current.revision());
            return new AuctionMutation(true, "ACTIVE", active);
        });
    }

    private CompletionStage<AuctionMutation> reject(AuctionListing listing, String reason, String operationKey) {
        return store.transact("auction-reject:" + operationKey, tx -> {
            var current = listings.get(tx, listing.id().toString()).orElseThrow();
            if (current.value().status() != AuctionListing.Status.PREPARING) return new AuctionMutation(false, "STATE_CONFLICT", current.value());
            if (listing.listingFeeMinor() > 0) EconomyTransactionSupport.credit(tx, listing.sellerId(), currency,
                    listing.listingFeeMinor(), operationKey + ":fee-refund", "magiccore", "auction-rejected:" + reason, clock.instant());
            AuctionListing rejected = copy(current.value(), AuctionListing.Status.REJECTED, null, "", null, clock.instant(), operationKey);
            listings.put(tx, listing.id().toString(), rejected, current.revision());
            return new AuctionMutation(false, reason, rejected);
        });
    }

    @Override public CompletionStage<AuctionMutation> purchase(UUID buyerId, UUID listingId, String operationKey) {
        return store.transact("auction-purchase:" + operationKey, tx -> {
            var current = listings.get(tx, listingId.toString()).orElseThrow(() -> new IllegalStateException("LISTING_NOT_FOUND"));
            if (!IdempotencyKeys.reserve(tx, "auction-purchase", operationKey)) return new AuctionMutation(false, "REPLAY", current.value());
            AuctionListing listing = current.value();
            if (listing.sellerId().equals(buyerId)) throw new IllegalArgumentException("CANNOT_BUY_OWN_LISTING");
            if (!listing.purchasableAt(clock.instant())) throw new IllegalStateException("LISTING_UNAVAILABLE");
            EconomyTransactionSupport.transfer(tx, buyerId, listing.sellerId(), currency, listing.priceMinor(),
                    operationKey, clock.instant());
            AuctionListing sold = copy(listing, AuctionListing.Status.SOLD, buyerId, listing.itemPayloadBase64(),
                    listing.activeAt(), clock.instant(), operationKey);
            listings.put(tx, listingId.toString(), sold, current.revision());
            DeliveryTransactionSupport.enqueue(tx, MailboxDelivery.pending(UUID.randomUUID(), buyerId,
                    operationKey, "magiccore/auction-item-v1", java.util.Base64.getDecoder().decode(listing.itemPayloadBase64()), clock.instant()));
            return new AuctionMutation(true, "PURCHASED", sold);
        }).thenApply(result -> {
            if (result.applied()) events.publish(new AuctionCompleted(result.listing().id(), result.listing().sellerId(),
                    result.listing().buyerId(), result.listing().priceMinor(), result.listing().currency(), operationKey, clock.instant()));
            return result;
        });
    }

    @Override public CompletionStage<AuctionMutation> cancel(UUID sellerId, UUID listingId, String operationKey) {
        return closeAndReturn(sellerId, listingId, operationKey, AuctionListing.Status.CANCELLED);
    }

    @Override public CompletionStage<Integer> expire(String operationKey, int limit) {
        return store.transact("auction-expire:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "auction-expire", operationKey)) return 0;
            int changed = 0;
            for (var record : listings.scan(tx, null, Math.min(1000, Math.max(1, limit * 4)))) {
                AuctionListing listing = record.value();
                if (changed >= limit) break;
                if (listing.status() != AuctionListing.Status.ACTIVE || listing.expiresAt().isAfter(clock.instant())) continue;
                AuctionListing expired = copy(listing, AuctionListing.Status.EXPIRED, null, listing.itemPayloadBase64(),
                        listing.activeAt(), clock.instant(), operationKey);
                listings.put(tx, listing.id().toString(), expired, record.revision());
                enqueueReturn(tx, expired, operationKey + ":" + listing.id());
                changed++;
            }
            return changed;
        });
    }

    private CompletionStage<AuctionMutation> closeAndReturn(UUID sellerId, UUID listingId, String operationKey,
                                                              AuctionListing.Status status) {
        return store.transact("auction-close:" + operationKey, tx -> {
            var current = listings.get(tx, listingId.toString()).orElseThrow(() -> new IllegalStateException("LISTING_NOT_FOUND"));
            if (!IdempotencyKeys.reserve(tx, "auction-close", operationKey)) return new AuctionMutation(false, "REPLAY", current.value());
            AuctionListing listing = current.value();
            if (!listing.sellerId().equals(sellerId)) throw new SecurityException("NOT_SELLER");
            if (listing.status() != AuctionListing.Status.ACTIVE) throw new IllegalStateException("LISTING_UNAVAILABLE");
            AuctionListing closed = copy(listing, status, null, listing.itemPayloadBase64(), listing.activeAt(), clock.instant(), operationKey);
            listings.put(tx, listingId.toString(), closed, current.revision());
            enqueueReturn(tx, closed, operationKey);
            return new AuctionMutation(true, status.name(), closed);
        });
    }

    private void enqueueReturn(com.magicstudios.magiccore.storage.DataTransaction tx, AuctionListing listing,
                               String operationKey) throws Exception {
        DeliveryTransactionSupport.enqueue(tx, MailboxDelivery.pending(UUID.randomUUID(), listing.sellerId(),
                operationKey, "magiccore/auction-item-v1", java.util.Base64.getDecoder().decode(listing.itemPayloadBase64()), clock.instant()));
    }

    @Override public CompletionStage<AuctionPage> search(String query, String category, Sort sort, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("Invalid page");
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        String categoryId = category == null ? "" : normalize(category);
        Comparator<AuctionListing> comparator = switch (sort) {
            case PRICE_ASC -> Comparator.comparingLong(AuctionListing::priceMinor).thenComparing(AuctionListing::id);
            case PRICE_DESC -> Comparator.comparingLong(AuctionListing::priceMinor).reversed().thenComparing(AuctionListing::id);
            case EXPIRING -> Comparator.comparing(AuctionListing::expiresAt).thenComparing(AuctionListing::id);
            case NEWEST -> Comparator.comparing(AuctionListing::createdAt).reversed().thenComparing(AuctionListing::id);
        };
        return store.read(reader -> {
            List<AuctionListing> matches = listings.scan(reader, null, 1000).stream().map(RecordRepository.VersionedValue::value)
                    .filter(listing -> listing.purchasableAt(clock.instant()))
                    .filter(listing -> categoryId.isEmpty() || listing.category().equals(categoryId))
                    .filter(listing -> needle.isEmpty() || listing.fingerprint().material().toLowerCase(Locale.ROOT).contains(needle))
                    .sorted(comparator).toList();
            return new AuctionPage(matches.stream().skip((long) page * pageSize).limit(pageSize).toList(), page, pageSize, matches.size());
        });
    }

    @Override public CompletionStage<List<AuctionListing>> history(UUID playerId, int limit) {
        return store.read(reader -> listings.scan(reader, null, 1000).stream().map(RecordRepository.VersionedValue::value)
                .filter(listing -> listing.sellerId().equals(playerId) || playerId.equals(listing.buyerId()))
                .sorted(Comparator.comparing(AuctionListing::createdAt).reversed()).limit(limit).toList());
    }

    private void validateCreate(String category, int quantity, long price, Duration duration) {
        if (!categories.contains(normalize(category))) throw new IllegalArgumentException("UNKNOWN_CATEGORY");
        if (quantity < 1 || quantity > 2304) throw new IllegalArgumentException("INVALID_QUANTITY");
        if (price < minimumPriceMinor || price > maximumPriceMinor) throw new IllegalArgumentException("INVALID_PRICE");
        if (duration.compareTo(minimumDuration) < 0 || duration.compareTo(maximumDuration) > 0)
            throw new IllegalArgumentException("INVALID_DURATION");
    }
    private static String normalize(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,24}")) throw new IllegalArgumentException("INVALID_CATEGORY");
        return value.toLowerCase(Locale.ROOT);
    }
    private static AuctionListing copy(AuctionListing value, AuctionListing.Status status, UUID buyer,
                                       String payload, java.time.Instant activeAt, java.time.Instant closedAt,
                                       String operationKey) {
        return new AuctionListing(value.id(), value.sellerId(), buyer, value.category(), value.fingerprint(),
                value.quantity(), payload, value.currency(), value.priceMinor(), value.listingFeeMinor(), status,
                value.createdAt(), activeAt, value.expiresAt(), closedAt, operationKey);
    }
    private record RemovalOutcome(InventoryRemovalPort.RemovalReceipt receipt, Throwable failure) { }
}
