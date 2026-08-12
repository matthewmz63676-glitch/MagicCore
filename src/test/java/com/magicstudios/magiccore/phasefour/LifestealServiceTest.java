package com.magicstudios.magiccore.phasefour;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.lifesteal.PersistentLifestealService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class LifestealServiceTest{
 @Test void transferIsIdempotentAntiFarmLimitedAndEliminatesAtFinalHeart(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"heart-test"));
  try{var clock=new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));var service=new PersistentLifestealService(store,new DomainEventBus(),clock,2,1,3,2,Duration.ofHours(1),PersistentLifestealService.NonPlayerDeathPolicy.LOSE_HEART,"NETHER_STAR","<red>Heart");
   var mailbox=new PersistentDeliveryMailbox(store,clock);UUID killer=UUID.randomUUID(),victim=UUID.randomUUID();
   var first=new VerifiedPlayerKill(UUID.randomUUID(),killer,victim,"TEST",clock.instant());assertThat(service.transfer(first,"kill-1").toCompletableFuture().join().code()).isEqualTo("TRANSFERRED");
   assertThat(service.transfer(first,"kill-1-replay").toCompletableFuture().join().code()).isEqualTo("KILL_REPLAY");
   var farm=new VerifiedPlayerKill(UUID.randomUUID(),killer,victim,"TEST",clock.instant());assertThat(service.transfer(farm,"kill-2").toCompletableFuture().join().code()).isEqualTo("ANTI_FARM_COOLDOWN");
   clock.advance(Duration.ofHours(1));var finalKill=new VerifiedPlayerKill(UUID.randomUUID(),killer,victim,"TEST",clock.instant());
   var eliminated=service.transfer(finalKill,"kill-3").toCompletableFuture().join();assertThat(eliminated.code()).isEqualTo("ELIMINATED");
   assertThat(eliminated.player().hearts()).isEqualTo(3);assertThat(eliminated.other().eliminated()).isTrue();
   assertThat(mailbox.pending(killer,10).toCompletableFuture().join()).hasSize(1);
   assertThat(service.revive(victim,"revive-1").toCompletableFuture().join().player().hearts()).isEqualTo(2);
 }finally{store.close();}}
 @Test void withdrawalUsesDurableMailboxAndConsumptionEnforcesMaximum(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"heart-item-test"));
  try{var clock=new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));var events=new DomainEventBus();var service=new PersistentLifestealService(store,events,clock,2,1,3,2,Duration.ZERO,PersistentLifestealService.NonPlayerDeathPolicy.IGNORE,"NETHER_STAR","<red>Heart</red>");
   var mailbox=new PersistentDeliveryMailbox(store,clock);UUID player=UUID.randomUUID();
   var withdrawn=service.withdraw(player,"withdraw-1").toCompletableFuture().join();assertThat(withdrawn.code()).isEqualTo("WITHDRAWN");assertThat(withdrawn.player().hearts()).isEqualTo(1);
   assertThat(mailbox.pending(player,10).toCompletableFuture().join()).singleElement().satisfies(delivery->assertThat(delivery.payloadType()).isEqualTo("magiccore/heart-v1"));
   assertThat(service.consume(player,"consume-1").toCompletableFuture().join().player().hearts()).isEqualTo(2);
   assertThat(service.consume(player,"consume-2").toCompletableFuture().join().player().hearts()).isEqualTo(3);
   assertThatThrownBy(()->service.consume(player,"consume-3").toCompletableFuture().join()).hasRootCauseMessage("MAXIMUM_HEARTS_REACHED");
  }finally{store.close();}}
 private static final class MutableClock extends Clock{private Instant instant;MutableClock(Instant i){instant=i;}void advance(Duration d){instant=instant.plus(d);}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId z){return this;}@Override public Instant instant(){return instant;}}
}
