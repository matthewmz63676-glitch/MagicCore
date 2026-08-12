package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.crates.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalCrateServiceTest {
    @Test void completedOpenReplaysWithoutCallingProviderTwice(){var store=store("external-open");try{FakeProvider provider=new FakeProvider();var service=service(store,provider);UUID player=UUID.randomUUID();
        var first=service.open(player,"VOTE",1,"open-1").toCompletableFuture().join();var replay=service.open(player,"VOTE",1,"open-1").toCompletableFuture().join();
        assertThat(first.applied()).isTrue();assertThat(replay.code()).isEqualTo("REPLAY");assertThat(provider.opens).hasValue(1);assertThat(service.history(player,10).toCompletableFuture().join()).hasSize(1);
    }finally{store.close();}}

    @Test void ambiguousExternalFailureRequiresReconciliationInsteadOfDuplicatingGrant(){var store=store("external-grant");try{FakeProvider provider=new FakeProvider();provider.failGrant=true;var service=service(store,provider);UUID player=UUID.randomUUID();
        assertThatThrownBy(()->service.grantKeys(player,"VOTE_KEY",2,"grant-1").toCompletableFuture().join()).hasRootCauseMessage("simulated provider failure");
        provider.failGrant=false;assertThatThrownBy(()->service.grantKeys(player,"VOTE_KEY",2,"grant-1").toCompletableFuture().join()).hasRootCauseMessage("EXTERNAL_CRATE_RECONCILIATION_REQUIRED:grant-1");
        assertThat(provider.grants).hasValue(1);
    }finally{store.close();}}

    private static ExternalCrateService service(InMemoryTransactionalDataStore store,FakeProvider provider){var crate=new CrateDefinition("VOTE","Vote",new CrateCost(CrateCost.Type.KEY,"VOTE_KEY",1),1,List.of(new CrateReward("X",CrateReward.Type.ITEM,1,"COMMON","STONE",1,"","",0,"",0)),List.of());
        return new ExternalCrateService(store,provider,Runnable::run,new DomainEventBus(),Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC),Map.of("VOTE",crate));}
    private static InMemoryTransactionalDataStore store(String name){return new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,name));}
    private static final class FakeProvider implements ExternalCrateProvider{private final AtomicInteger opens=new AtomicInteger(),grants=new AtomicInteger();private boolean failGrant;private long keys;
        public String id(){return "FAKE";}public boolean available(){return true;}public boolean hasCrate(String id){return id.equals("VOTE");}public long keyBalance(UUID player,String key){return keys;}
        public void grantKeys(UUID player,String key,long amount){grants.incrementAndGet();if(failGrant)throw new IllegalStateException("simulated provider failure");keys+=amount;}public boolean open(UUID player,String crate){opens.incrementAndGet();return true;}}
}
