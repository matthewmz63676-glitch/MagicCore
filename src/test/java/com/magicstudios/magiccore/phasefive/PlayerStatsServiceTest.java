package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.statistics.PersistentPlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.StatsMetric;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerStatsServiceTest {
    @Test void verifiedKillsAreIdempotentAndLeaderboardsUseAuthoritativeStats() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"stats-test"));
        try{var clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);var service=new PersistentPlayerStatsService(store,new DomainEventBus(),clock);
            UUID killer=UUID.randomUUID(),victim=UUID.randomUUID();var kill=new VerifiedPlayerKill(UUID.randomUUID(),killer,victim,"TEST",clock.instant());
            service.recordKill(kill,"kill-1").toCompletableFuture().join();service.recordKill(kill,"kill-replay").toCompletableFuture().join();
            service.addPlaytime(killer,90,"session-1").toCompletableFuture().join();
            assertThat(service.stats(killer).toCompletableFuture().join().kills()).isEqualTo(1);
            assertThat(service.stats(victim).toCompletableFuture().join().deaths()).isEqualTo(1);
            assertThat(service.leaderboard(StatsMetric.KILLS,10).toCompletableFuture().join()).first().satisfies(entry->{assertThat(entry.playerId()).isEqualTo(killer);assertThat(entry.value()).isEqualTo(1);});
            assertThat(service.leaderboard(StatsMetric.PLAYTIME,10).toCompletableFuture().join()).first().extracting(entry->entry.value()).isEqualTo(90L);
        }finally{store.close();}
    }
}
