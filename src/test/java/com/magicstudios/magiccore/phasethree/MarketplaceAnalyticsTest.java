package com.magicstudios.magiccore.phasethree;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.auction.PersistentAuctionService;
import com.magicstudios.magiccore.modules.bounties.PersistentBountyService;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.marketplace.PersistentMarketplaceAnalyticsService;
import com.magicstudios.magiccore.modules.orders.PersistentOrderService;
import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;
class MarketplaceAnalyticsTest{
 @Test void snapshotAndLeaderboardDeriveFromAuthoritativeRecords(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"market-analytics"));
  try{Clock clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC);var coins=new CurrencyDefinition("COINS","Coins","$",2,1000,1_000_000);
   CapabilityService limits=new CapabilityService(){public java.util.concurrent.CompletionStage<Boolean>has(UUID i,String c){return CompletableFuture.completedFuture(true);}public java.util.concurrent.CompletionStage<Integer>limit(UUID i,String l){return CompletableFuture.completedFuture(10);}public java.util.concurrent.CompletionStage<Boolean>canTarget(UUID a,UUID t){return CompletableFuture.completedFuture(true);}};
   InventoryRemovalPort inventory=(p,f,q,o)->CompletableFuture.completedFuture(new InventoryRemovalPort.RemovalReceipt(true,"REMOVED",Base64.getEncoder().encodeToString(new byte[]{1})));
   UUID seller=UUID.randomUUID(),buyer=UUID.randomUUID(),creator=UUID.randomUUID(),target=UUID.randomUUID();var item=ItemFingerprint.of("STONE",new byte[]{2});
   new PersistentAuctionService(store,limits,inventory,coins,new DomainEventBus(),clock,Duration.ofMinutes(5),Duration.ofDays(7),1,10000,0,Set.of("blocks"))
           .create(seller,"blocks",item,1,300,Duration.ofHours(1),"auction").toCompletableFuture().join();
   new PersistentOrderService(store,limits,inventory,coins,clock,Duration.ofMinutes(5),Duration.ofDays(7),1,10000,Set.of("blocks"))
           .create(buyer,"blocks",item,2,100,Duration.ofHours(1),"order").toCompletableFuture().join();
   new PersistentBountyService(store,coins,new DomainEventBus(),clock,100,10000,0,100).create(creator,target,100,"bounty").toCompletableFuture().join();
   var analytics=new PersistentMarketplaceAnalyticsService(store,clock,Map.of("COINS",coins));var snapshot=analytics.snapshot().toCompletableFuture().join();
   assertThat(snapshot.activeAuctions()).isEqualTo(1);assertThat(snapshot.activeAuctionValueMinor()).isEqualTo(300);
   assertThat(snapshot.openOrders()).isEqualTo(1);assertThat(snapshot.orderEscrowMinor()).isEqualTo(200);
   assertThat(snapshot.activeBounties()).isEqualTo(1);assertThat(snapshot.bountyEscrowMinor()).isEqualTo(100);
   assertThat(snapshot.sunkMinor()).isEqualTo(300);assertThat(analytics.balanceLeaderboard("COINS",10).toCompletableFuture().join()).extracting(e->e.playerId()).contains(buyer,creator);
  }finally{store.close();}}
}
