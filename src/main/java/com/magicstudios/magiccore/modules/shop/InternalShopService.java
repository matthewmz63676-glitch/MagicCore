package com.magicstudios.magiccore.modules.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InternalShopService implements ShopService {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final TransactionalDataStore store;
    private final CurrencyDefinition currency;
    private final Clock clock;
    private final Map<String, ShopProduct> products;

    public InternalShopService(TransactionalDataStore store, CurrencyDefinition currency, Clock clock,
                               List<ShopProduct> products) {
        this.store = store;
        this.currency = currency;
        this.clock = clock;
        this.products = products.stream().collect(Collectors.toUnmodifiableMap(ShopProduct::id, Function.identity()));
    }

    @Override public List<ShopProduct> products() { return List.copyOf(products.values()); }

    @Override
    public CompletionStage<PurchaseResult> buy(UUID playerId, String productId, int quantity, String operationKey) {
        ShopProduct product = products.get(productId.toLowerCase(Locale.ROOT));
        if (product == null) throw new IllegalArgumentException("PRODUCT_NOT_FOUND");
        if (quantity < 1 || quantity > 64) throw new IllegalArgumentException("Quantity must be 1..64");
        long price = Math.multiplyExact(product.buyPriceMinor(), quantity);
        return store.transact("shop-buy:" + operationKey, tx -> {
            long existing = EconomyTransactionSupport.balance(tx, playerId, currency).minorUnits();
            if (!IdempotencyKeys.reserve(tx, "shop-buy", operationKey))
                return new PurchaseResult(false, "REPLAY", 0, existing);
            var applied = EconomyTransactionSupport.credit(tx, playerId, currency, -price, operationKey,
                    playerId.toString(), "shop-buy:" + product.id(), clock.instant());
            PurchasePayload payload = new PurchasePayload(product.id(), product.material(),
                    Math.multiplyExact(product.amount(), quantity), product.itemDataBase64());
            DeliveryTransactionSupport.enqueue(tx, MailboxDelivery.pending(UUID.randomUUID(), playerId, operationKey,
                    "magiccore/shop-purchase-v1", JSON.writeValueAsBytes(payload), clock.instant()));
            return new PurchaseResult(true, "PURCHASED", price, applied.afterMinor());
        });
    }

    public record PurchasePayload(String productId, String material, int amount, String itemDataBase64) { }
}
