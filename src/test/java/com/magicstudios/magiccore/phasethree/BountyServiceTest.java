package com.magicstudios.magiccore.phasethree;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.bounties.PersistentBountyService;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class BountyServiceTest{
 @Test void taxedEscrowPaysOnlyVerifiedKillOnce(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"bounty-test"));
  try{Clock clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC);var coins=new CurrencyDefinition("COINS","Coins","$",2,1000,1_000_000);
   var service=new PersistentBountyService(store,coins,new DomainEventBus(),clock,100,10_000,500,100);
   UUID creator=UUID.randomUUID(),target=UUID.randomUUID(),killer=UUID.randomUUID(),killId=UUID.randomUUID();
   var created=service.create(creator,target,400,"bounty-1").toCompletableFuture().join();
   assertThat(created.contribution().taxMinor()).isEqualTo(20);assertThat(balance(store,creator,coins)).isEqualTo(580);
   var kill=new VerifiedPlayerKill(killId,killer,target,"TEST_VERIFIED_KILL",clock.instant());
   assertThat(service.claim(kill,"claim-1").toCompletableFuture().join().applied()).isTrue();
   assertThat(service.claim(kill,"claim-duplicate").toCompletableFuture().join().applied()).isFalse();
   assertThat(balance(store,killer,coins)).isEqualTo(1400);assertThat(service.active(target).toCompletableFuture().join()).isEmpty();
   assertThat(service.claimHistory(killer,10).toCompletableFuture().join()).singleElement().extracting(c->c.killEventId()).isEqualTo(killId);
  }finally{store.close();}}
 private static long balance(InMemoryTransactionalDataStore store,UUID id,CurrencyDefinition currency){return store.read(r->EconomyTransactionSupport.balance(r,id,currency).minorUnits()).toCompletableFuture().join();}
}
