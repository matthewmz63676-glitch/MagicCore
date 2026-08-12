package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.economy.*;
import com.magicstudios.magiccore.modules.gemshop.*;
import com.magicstudios.magiccore.modules.statistics.PersistentPlayerStatsService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GemShopServiceTest {
    @Test void quoteChecksPrerequisitesAndConfirmationAtomicallyDebitsIntoRecoveryDelivery() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"gemshop-test"));
        try{Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC);var events=new DomainEventBus();UUID player=UUID.randomUUID();
            CurrencyDefinition gems=new CurrencyDefinition("GEMS","Gems","◆",0,0,1000);var economy=new PersistentEconomyService(store,events,"GEMS",Map.of("GEMS",gems),clock);economy.adjust(player,new Money("GEMS",100),"admin","seed","seed").toCompletableFuture().join();
            var stats=new PersistentPlayerStatsService(store,events,clock);stats.addPlaytime(player,60,"playtime").toCompletableFuture().join();
            CapabilityService capabilities=capabilities(true);GemProduct product=new GemProduct("GEM_DIAMONDS","RESOURCES","<aqua>Diamonds</aqua>","DIAMOND",8,"",25,"SHOP_ACCESS",60,0);
            var service=new PersistentGemShopService(store,gems,capabilities,stats,clock,Duration.ofSeconds(30),List.of(product));GemShopQuote quote=service.quote(player,"gem_diamonds","quote").toCompletableFuture().join();
            GemShopReceipt receipt=service.confirm(player,quote.id(),"confirm").toCompletableFuture().join();assertThat(receipt.chargedMinor()).isEqualTo(25);assertThat(receipt.balanceAfterMinor()).isEqualTo(75);
            assertThat(new PersistentDeliveryMailbox(store,clock).pending(player,10).toCompletableFuture().join()).singleElement().satisfies(delivery->{assertThat(delivery.id()).isEqualTo(receipt.deliveryId());assertThat(delivery.payloadType()).isEqualTo("magiccore/shop-purchase-v1");});
            assertThat(service.confirm(player,quote.id(),"confirm-replay").toCompletableFuture().join().id()).isEqualTo(receipt.id());
            assertThat(economy.balance(player,"GEMS").toCompletableFuture().join().minorUnits()).isEqualTo(75);
        }finally{store.close();}}

    @Test void deniedCapabilityPreventsQuoteAndAnyCharge(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,32,"gemshop-denied"));try{Clock clock=Clock.systemUTC();var stats=new PersistentPlayerStatsService(store,new DomainEventBus(),clock);UUID player=UUID.randomUUID();GemProduct product=new GemProduct("LOCKED","PERKS","Locked","DIAMOND",1,"",1,"VIP_ACCESS",0,0);var service=new PersistentGemShopService(store,new CurrencyDefinition("GEMS","Gems","◆",0,0,100),capabilities(false),stats,clock,Duration.ofSeconds(30),List.of(product));assertThatThrownBy(()->service.quote(player,"LOCKED","denied").toCompletableFuture().join()).hasRootCauseMessage("GEMSHOP_PREREQUISITE_NOT_MET");}finally{store.close();}}

    private static CapabilityService capabilities(boolean allowed){return new CapabilityService(){public java.util.concurrent.CompletionStage<Boolean>has(UUID id,String capability){return CompletableFuture.completedFuture(allowed);}public java.util.concurrent.CompletionStage<Integer>limit(UUID id,String limit){return CompletableFuture.completedFuture(0);}public java.util.concurrent.CompletionStage<Boolean>canTarget(UUID actor,UUID target){return CompletableFuture.completedFuture(false);}};}
}
