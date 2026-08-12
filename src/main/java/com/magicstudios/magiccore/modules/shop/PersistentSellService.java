package com.magicstudios.magiccore.modules.shop;

import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Durable sell saga: reserve once, exact entity-thread removal, then atomic credit/settlement. */
public final class PersistentSellService implements SellService {
    private final TransactionalDataStore store;
    private final InventoryRemovalPort inventory;
    private final CurrencyDefinition currency;
    private final Clock clock;
    private final Duration quoteLifetime;
    private final Map<String, ShopProduct> products;
    private final RecordRepository<SellQuote> quotes = new RecordRepository<>("shop.sell", SellQuote.class);

    public PersistentSellService(TransactionalDataStore store, InventoryRemovalPort inventory,
                                 CurrencyDefinition currency, Clock clock, Duration quoteLifetime,
                                 List<ShopProduct> products) {
        this.store = store; this.inventory = inventory; this.currency = currency; this.clock = clock;
        this.quoteLifetime = quoteLifetime;
        this.products = products.stream().collect(Collectors.toUnmodifiableMap(ShopProduct::id, Function.identity()));
    }

    @Override
    public CompletionStage<SellQuote> quote(UUID playerId, String productId, ItemFingerprint fingerprint,
                                            int quantity, String operationKey) {
        ShopProduct product = requireProduct(productId);
        if (!product.material().equalsIgnoreCase(fingerprint.material())) throw new IllegalArgumentException("ITEM_NOT_VALUED_AS_PRODUCT");
        if (quantity < 1 || quantity % product.amount() != 0) throw new IllegalArgumentException("INVALID_SELL_QUANTITY");
        long credit = Math.multiplyExact(product.sellPriceMinor(), quantity / product.amount());
        return store.transact("sell-quote:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "sell-quote", operationKey))
                throw new IllegalStateException("DUPLICATE_QUOTE_OPERATION");
            var now = clock.instant();
            SellQuote quote = new SellQuote(UUID.randomUUID(), playerId, product.id(), fingerprint, quantity, credit,
                    SellQuote.Status.QUOTED, null, "", now, now.plus(quoteLifetime), now);
            quotes.put(tx, quote.id().toString(), quote, 0);
            return quote;
        });
    }

    @Override
    public CompletionStage<SellResult> execute(UUID playerId, UUID quoteId, String operationKey) {
        return reserve(playerId, quoteId, operationKey).thenCompose(reserved -> {
            if (reserved == null) return CompletableFuture.completedFuture(new SellResult(false, "REPLAY_OR_UNAVAILABLE", 0, 0));
            return inventory.removeExact(playerId, reserved.fingerprint(), reserved.itemQuantity(), operationKey)
                    .thenCompose(receipt -> receipt.removed() ? markRemoved(reserved, receipt).thenCompose(this::settle)
                            : reject(reserved, receipt.code()));
        });
    }

    private CompletionStage<SellQuote> reserve(UUID playerId, UUID quoteId, String operationKey) {
        return store.transact("sell-reserve:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "sell-execute", operationKey)) return null;
            var current = quotes.get(tx, quoteId.toString()).orElseThrow(() -> new IllegalStateException("QUOTE_NOT_FOUND"));
            SellQuote quote = current.value();
            if (!quote.playerId().equals(playerId)) throw new SecurityException("QUOTE_OWNER_MISMATCH");
            if (quote.status() != SellQuote.Status.QUOTED || !quote.expiresAt().isAfter(clock.instant())) return null;
            SellQuote reserved = copy(quote, SellQuote.Status.REMOVING, operationKey);
            quotes.put(tx, quoteId.toString(), reserved, current.revision());
            return reserved;
        });
    }

    private CompletionStage<SellQuote> markRemoved(SellQuote quote, InventoryRemovalPort.RemovalReceipt receipt) {
        return store.transact("sell-removed:" + quote.executionKey(), tx -> {
            var current = quotes.get(tx, quote.id().toString()).orElseThrow();
            if (current.value().status() != SellQuote.Status.REMOVING) throw new IllegalStateException("SELL_STATE_CONFLICT");
            SellQuote before = current.value();
            SellQuote removed = new SellQuote(before.id(), before.playerId(), before.productId(), before.fingerprint(),
                    before.itemQuantity(), before.creditMinor(), SellQuote.Status.REMOVED, before.executionKey(),
                    receipt.recoveryPayloadBase64(), before.createdAt(), before.expiresAt(), clock.instant());
            quotes.put(tx, quote.id().toString(), removed, current.revision());
            return removed;
        });
    }

    private CompletionStage<SellResult> settle(SellQuote quote) {
        return store.transact("sell-settle:" + quote.executionKey(), tx -> {
            var current = quotes.get(tx, quote.id().toString()).orElseThrow();
            if (current.value().status() == SellQuote.Status.SETTLED)
                return new SellResult(false, "REPLAY", 0, EconomyTransactionSupport.balance(tx, quote.playerId(), currency).minorUnits());
            if (current.value().status() != SellQuote.Status.REMOVED) throw new IllegalStateException("SELL_NOT_REMOVED");
            var applied = EconomyTransactionSupport.credit(tx, quote.playerId(), currency, quote.creditMinor(),
                    quote.executionKey(), quote.playerId().toString(), "shop-sell:" + quote.productId(), clock.instant());
            quotes.put(tx, quote.id().toString(), copy(current.value(), SellQuote.Status.SETTLED,
                    current.value().executionKey()), current.revision());
            return new SellResult(true, "SOLD", quote.creditMinor(), applied.afterMinor());
        });
    }

    private CompletionStage<SellResult> reject(SellQuote quote, String reason) {
        return store.transact("sell-reject:" + quote.executionKey(), tx -> {
            var current = quotes.get(tx, quote.id().toString()).orElseThrow();
            quotes.put(tx, quote.id().toString(), copy(current.value(), SellQuote.Status.REJECTED,
                    current.value().executionKey()), current.revision());
            return new SellResult(false, reason, 0, EconomyTransactionSupport.balance(tx, quote.playerId(), currency).minorUnits());
        });
    }

    private SellQuote copy(SellQuote quote, SellQuote.Status status, String executionKey) {
        return new SellQuote(quote.id(), quote.playerId(), quote.productId(), quote.fingerprint(), quote.itemQuantity(),
                quote.creditMinor(), status, executionKey, quote.recoveryPayloadBase64(), quote.createdAt(),
                quote.expiresAt(), clock.instant());
    }
    private ShopProduct requireProduct(String id) {
        ShopProduct product = products.get(id.toLowerCase(Locale.ROOT));
        if (product == null || product.sellPriceMinor() <= 0) throw new IllegalArgumentException("PRODUCT_NOT_SELLABLE");
        return product;
    }
}
