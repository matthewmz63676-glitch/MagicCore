package com.magicstudios.magiccore.phasethree;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.modules.orders.PersistentOrderService;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {
    @Test
    void partialFulfillmentPaysSellerDeliversBuyerAndRefundsOnlyUnusedEscrow() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"order-test"));
        try{
            Clock clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC);
            CurrencyDefinition coins=new CurrencyDefinition("COINS","Coins","$",2,1_000,1_000_000);
            InventoryRemovalPort inventory=(player,fingerprint,quantity,operation)->CompletableFuture.completedFuture(
                    new InventoryRemovalPort.RemovalReceipt(true,"REMOVED", Base64.getEncoder().encodeToString(new byte[]{1,2})));
            var orders=new PersistentOrderService(store,limits(2),inventory,coins,clock,Duration.ofMinutes(5),Duration.ofDays(7),1,10_000,Set.of("blocks"));
            var mailbox=new PersistentDeliveryMailbox(store,clock);UUID buyer=UUID.randomUUID(),seller=UUID.randomUUID();
            var order=orders.create(buyer,"blocks", ItemFingerprint.of("STONE",new byte[]{3}),10,50,Duration.ofHours(1),"order-1")
                    .toCompletableFuture().join().order();
            var partial=orders.fulfill(seller,order.id(),4,"fill-1").toCompletableFuture().join();
            assertThat(partial.code()).isEqualTo("PARTIAL");
            assertThat(partial.order().filledQuantity()).isEqualTo(4);
            assertThat(partial.order().escrowRemainingMinor()).isEqualTo(300);
            assertThat(mailbox.pending(buyer,10).toCompletableFuture().join()).hasSize(1);
            assertThat(balance(store,seller,coins)).isEqualTo(1_200);
            assertThat(balance(store,buyer,coins)).isEqualTo(500);
            assertThat(orders.cancel(buyer,order.id(),"cancel-1").toCompletableFuture().join().applied()).isTrue();
            assertThat(balance(store,buyer,coins)).isEqualTo(800);
        }finally{store.close();}
    }
    private static long balance(InMemoryTransactionalDataStore store,UUID player,CurrencyDefinition currency){return store.read(r->EconomyTransactionSupport.balance(r,player,currency).minorUnits()).toCompletableFuture().join();}
    private static CapabilityService limits(int limit){return new CapabilityService(){
        @Override public java.util.concurrent.CompletionStage<Boolean> has(UUID id,String c){return CompletableFuture.completedFuture(true);}
        @Override public java.util.concurrent.CompletionStage<Integer> limit(UUID id,String l){return CompletableFuture.completedFuture(limit);}
        @Override public java.util.concurrent.CompletionStage<Boolean> canTarget(UUID a,UUID t){return CompletableFuture.completedFuture(true);}
    };}
}
