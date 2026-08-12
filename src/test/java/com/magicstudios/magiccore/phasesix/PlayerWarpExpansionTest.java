package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.economy.*;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.modules.playerwarps.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.protection.AllowAllProtectionService;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerWarpExpansionTest {
    @Test void searchFavoritesVisitsModerationAndSponsorshipAreAtomicAndDeterministic(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"playerwarp-expansion"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));CurrencyDefinition coins=new CurrencyDefinition("COINS","Coins","$",0,0,100000);UUID owner=UUID.randomUUID(),viewer=UUID.randomUUID();var economy=new PersistentEconomyService(store,new DomainEventBus(),"COINS",Map.of("COINS",coins),clock);economy.adjust(owner,new Money("COINS",5000),"admin","seed","seed").toCompletableFuture().join();var service=new PersistentPlayerWarpService(store,capabilities(),new AllowAllProtectionService(),clock,24,Set.of("SHOPS","FARMS"),Duration.ofDays(30),true,coins,1000,Duration.ofHours(1),Duration.ofDays(7),2,1);WorldPosition position=new WorldPosition(UUID.randomUUID(),"world",1,64,1,0,0);service.create(owner,"alpha","SHOPS",position,"create-alpha").toCompletableFuture().join();service.create(owner,"beta","FARMS",position,"create-beta").toCompletableFuture().join();assertThat(service.favorite(viewer,"beta",true,"favorite").toCompletableFuture().join()).isTrue();assertThat(service.prepareVisit("beta",viewer,"visit").toCompletableFuture().join().id()).isEqualTo("beta");WarpSponsorship sponsorship=service.sponsor(owner,"alpha",Duration.ofHours(1),"sponsor").toCompletableFuture().join();assertThat(economy.balance(owner,"COINS").toCompletableFuture().join().minorUnits()).isEqualTo(4000);var search=service.search(new PlayerWarpQuery("","",null,viewer,PlayerWarpQuery.Sort.SPONSORED,0,10)).toCompletableFuture().join();assertThat(search.getFirst().warp().id()).isEqualTo("alpha");assertThat(search.getFirst().promoted()).isTrue();assertThat(search.stream().filter(PlayerWarpView::favorite).map(value->value.warp().id())).containsExactly("beta");assertThatThrownBy(()->service.sponsor(owner,"beta",Duration.ofHours(1),"cap").toCompletableFuture().join()).hasRootCauseMessage("SPONSORSHIP_CAP_REACHED");clock.advance(Duration.ofMinutes(30));WarpSponsorship cancelled=service.cancelSponsorship(owner,sponsorship.id(),"cancel").toCompletableFuture().join();assertThat(cancelled.refundedMinor()).isEqualTo(500);assertThat(economy.balance(owner,"COINS").toCompletableFuture().join().minorUnits()).isEqualTo(4500);assertThat(service.moderate(UUID.randomUUID(),"beta",PlayerWarp.Status.SUSPENDED,"unsafe landing","moderate").toCompletableFuture().join().warp().status()).isEqualTo(PlayerWarp.Status.SUSPENDED);}finally{store.close();}}
    private static CapabilityService capabilities(){return new CapabilityService(){public java.util.concurrent.CompletionStage<Boolean>has(UUID id,String capability){return CompletableFuture.completedFuture(true);}public java.util.concurrent.CompletionStage<Integer>limit(UUID id,String limit){return CompletableFuture.completedFuture(5);}public java.util.concurrent.CompletionStage<Boolean>canTarget(UUID actor,UUID target){return CompletableFuture.completedFuture(true);}};}
    private static final class MutableClock extends Clock{private Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration duration){now=now.plus(duration);}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return this;}@Override public Instant instant(){return now;}}
}
