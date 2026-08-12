package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.config.model.AfkFile;
import com.magicstudios.magiccore.modules.afk.AfkEligibilitySnapshot;
import com.magicstudios.magiccore.modules.afk.PersistentShardService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShardServiceTest {
    @Test void awardsAreEligibleCappedDiminishingAndReplaySafe(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"shard-test"));try{var service=new PersistentShardService(store,new DomainEventBus(),Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC),
            new AfkFile.Policy(60,10,25,60,10,5000),new AfkFile.Eligibility(300,3,1,0,2500),Set.of("spawn_afk"));UUID player=UUID.randomUUID();
        var reconnect=new AfkEligibilitySnapshot("spawn_afk",600,10,10,1,0,0);assertThat(service.award(player,reconnect,"1").toCompletableFuture().join().code()).isEqualTo("RECONNECT_PROTECTION");
        var macro=new AfkEligibilitySnapshot("spawn_afk",600,600,10,1,0,3000);assertThat(service.award(player,macro,"2").toCompletableFuture().join().code()).isEqualTo("MACRO_RISK");
        var eligible=new AfkEligibilitySnapshot("spawn_afk",600,600,10,1,0,0);assertThat(service.award(player,eligible,"3").toCompletableFuture().join().awarded()).isEqualTo(10);assertThat(service.award(player,eligible,"3").toCompletableFuture().join().code()).isEqualTo("REPLAY");
        assertThat(service.award(player,eligible,"4").toCompletableFuture().join().awarded()).isEqualTo(5);assertThat(service.award(player,eligible,"5").toCompletableFuture().join().awarded()).isEqualTo(5);assertThat(service.award(player,eligible,"6").toCompletableFuture().join().awarded()).isEqualTo(5);assertThat(service.award(player,eligible,"7").toCompletableFuture().join().code()).isEqualTo("DAILY_CAP");
        assertThat(service.balance(player).toCompletableFuture().join().amount()).isEqualTo(25);assertThat(service.history(player,10).toCompletableFuture().join()).hasSize(4);
        assertThatThrownBy(()->service.adjust(player,-26,"admin","remove-too-many").toCompletableFuture().join()).hasRootCauseMessage("INSUFFICIENT_SHARDS");
    }finally{store.close();}}
}
