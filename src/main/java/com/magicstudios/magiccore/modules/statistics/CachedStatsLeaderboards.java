package com.magicstudios.magiccore.modules.statistics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedStatsLeaderboards {
    private final PlayerStatsService stats; private final Clock clock; private final Duration ttl;
    private final Map<String, Snapshot> cache = new ConcurrentHashMap<>();
    public CachedStatsLeaderboards(PlayerStatsService stats, Clock clock, Duration ttl) { this.stats=stats;this.clock=clock;this.ttl=ttl; }
    public CompletionStage<List<StatsLeaderboardEntry>> get(StatsMetric metric,int limit){String key=metric+":"+limit;Snapshot current=cache.get(key);
        if(current!=null&&current.expiresAt().isAfter(clock.instant()))return java.util.concurrent.CompletableFuture.completedFuture(current.entries());
        return stats.leaderboard(metric,limit).thenApply(entries->{cache.put(key,new Snapshot(entries,clock.instant().plus(ttl)));return entries;});}
    public void invalidate(){cache.clear();}
    private record Snapshot(List<StatsLeaderboardEntry>entries,Instant expiresAt){Snapshot{entries=List.copyOf(entries);}}
}
