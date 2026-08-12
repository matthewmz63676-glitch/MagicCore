package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.audit.PersistentAuditService;
import com.magicstudios.magiccore.config.model.ItemWorthFile;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.shop.*;
import com.magicstudios.magiccore.modules.worth.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdvancedSellServiceTest {
    @Test void batchQuoteRevalidatesRemovesOnceCreditsOnceAndPersistsReceipt(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"advanced-sell-test"));try{Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC);var audit=new PersistentAuditService(store);AtomicInteger removals=new AtomicInteger();
        BatchInventoryRemovalPort inventory=(player,lines,key)->{removals.incrementAndGet();assertThat(lines).hasSize(2);return CompletableFuture.completedFuture(new BatchInventoryRemovalPort.BatchRemovalReceipt(true,"REMOVED","recovery-payload"));};
        var service=new PersistentAdvancedSellService(store,inventory,valuation(),new CurrencyDefinition("COINS","Coins","$",2,500,100_000),audit,clock,Duration.ofSeconds(30));UUID player=UUID.randomUUID();
        var quote=service.quote(player,SellScope.ALL,"",List.of(input("minecraft:diamond","DIAMOND",2),input("minecraft:stone","STONE",16),protectedInput()),"quote-1").toCompletableFuture().join();
        assertThat(quote.lines()).hasSize(2);assertThat(quote.creditMinor()).isEqualTo(248);var receipt=service.execute(player,quote.id(),"execute-1").toCompletableFuture().join();assertThat(receipt.creditedMinor()).isEqualTo(248);assertThat(receipt.balanceAfterMinor()).isEqualTo(748);assertThat(receipt.economyTransactionId()).isNotNull();
        assertThat(service.execute(player,quote.id(),"execute-replay").toCompletableFuture().join()).isEqualTo(receipt);assertThat(removals).hasValue(1);assertThat(service.receipt(receipt.id()).toCompletableFuture().join()).contains(receipt);assertThat(service.history(player,10).toCompletableFuture().join()).containsExactly(receipt);assertThat(audit.recent(null,10).toCompletableFuture().join()).singleElement().extracting(event->event.action()).isEqualTo("ITEMS_SOLD");
    }finally{store.close();}}
    @Test void categoryAndStaleInventoryFailClosed(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,32,"advanced-sell-stale"));try{Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC);BatchInventoryRemovalPort inventory=(player,lines,key)->CompletableFuture.completedFuture(new BatchInventoryRemovalPort.BatchRemovalReceipt(false,"STALE_FINGERPRINT_OR_QUANTITY",""));var service=new PersistentAdvancedSellService(store,inventory,valuation(),new CurrencyDefinition("COINS","Coins","$",2,0,100_000),new PersistentAuditService(store),clock,Duration.ofSeconds(30));UUID player=UUID.randomUUID();
        var quote=service.quote(player,SellScope.CATEGORY,"blocks",List.of(input("minecraft:diamond","DIAMOND",2),input("minecraft:stone","STONE",16)),"quote-category").toCompletableFuture().join();assertThat(quote.lines()).singleElement().extracting(SellLine::category).isEqualTo("blocks");assertThatThrownBy(()->service.execute(player,quote.id(),"execute-stale").toCompletableFuture().join()).hasRootCauseMessage("STALE_FINGERPRINT_OR_QUANTITY");assertThat(removalBalance(store,player)).isZero();
    }finally{store.close();}}
    @Test void interruptedRemovalRequiresExplicitReconciliation(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,32,"advanced-sell-recovery"));try{Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC);CompletableFuture<Void>invoked=new CompletableFuture<>();CompletableFuture<BatchInventoryRemovalPort.BatchRemovalReceipt>hung=new CompletableFuture<>();BatchInventoryRemovalPort inventory=(player,lines,key)->{invoked.complete(null);return hung;};var service=new PersistentAdvancedSellService(store,inventory,valuation(),new CurrencyDefinition("COINS","Coins","$",2,0,100_000),new PersistentAuditService(store),clock,Duration.ofSeconds(30));UUID player=UUID.randomUUID();var quote=service.quote(player,SellScope.HAND,"",List.of(input("minecraft:diamond","DIAMOND",1)),"recovery-quote").toCompletableFuture().join();service.execute(player,quote.id(),"recovery-execute");invoked.join();assertThat(service.recoverRemoved().toCompletableFuture().join()).isEqualTo(1);var receipt=service.reconcile(quote.id(),true,"staff-confirmed-removal").toCompletableFuture().join();assertThat(receipt.creditedMinor()).isEqualTo(100);assertThat(receipt.balanceAfterMinor()).isEqualTo(100);
    }finally{store.close();}}
    private static ConfiguredItemValuationService valuation(){return new ConfiguredItemValuationService(new ItemWorthFile(1,"COINS",new ItemWorthFile.Policies("IGNORE",0,"IGNORE",0,"ALLOW","REJECT_NONEMPTY","USE_ENTRY",List.of(),List.of("magiccore:protected")),new ItemWorthFile.Presentation(true,true,true,"{amount} {currency}","No"),List.of(new ItemWorthFile.WorthEntry("diamond","minecraft:diamond","minerals",100),new ItemWorthFile.WorthEntry("stone","minecraft:stone","blocks",3))));}
    private static ValuationInput input(String id,String material,int amount){return new ValuationInput(id,"minecraft:"+material.toLowerCase(Locale.ROOT),ItemFingerprint.of(material,new byte[]{(byte)material.length()}),amount,0,0,0,false,0,"",Set.of());}
    private static ValuationInput protectedInput(){var base=input("minecraft:diamond","DIAMOND",1);return new ValuationInput(base.itemId(),base.materialId(),base.fingerprint(),1,0,0,0,false,0,"",Set.of("magiccore:protected"));}
    private static long removalBalance(InMemoryTransactionalDataStore store,UUID player){return store.read(reader->com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport.balance(reader,player,new CurrencyDefinition("COINS","Coins","$",2,0,100_000)).minorUnits()).toCompletableFuture().join();}
}
