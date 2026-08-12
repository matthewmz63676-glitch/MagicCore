package com.magicstudios.magiccore.phaseseven;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.config.model.EventsFile;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.events.PersistentVotePartyService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class EventPersistenceSoakTest {
    @Test void hundredsOfVerifiedEventsRemainBoundedAndReplaySafe(){try(var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(4,512,"event-soak"))){Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC);var reward=new EventsFile.EventReward("COINS",0);var policy=new EventsFile.VoteParty(true,25,"COUNT_AND_REWARD",false,new EventsFile.Pinata("world",0,64,0,"LLAMA",100,5,reward,reward,"<gold>Pinata</gold>"));var currency=new CurrencyDefinition("COINS","Coins","$",0,0,1_000_000);var service=new PersistentVotePartyService(store,clock,new DomainEventBus(),policy,Map.of("COINS",currency));for(int index=0;index<500;index++){String eventId="soak-"+index;UUID player=UUID.nameUUIDFromBytes(("player-"+(index%100)).getBytes(java.nio.charset.StandardCharsets.UTF_8));service.recordVerifiedVote(eventId,player,"SOAK",clock.instant(),index%2==0).toCompletableFuture().join();service.recordVerifiedVote(eventId,player,"SOAK",clock.instant(),index%2==0).toCompletableFuture().join();}assertThat(service.pendingParties(100).toCompletableFuture().join()).hasSize(20);assertThat(service.state().toCompletableFuture().join().count()).isZero();}}
}
