package com.magicstudios.magiccore.modules.display;

import com.magicstudios.magiccore.modules.lifesteal.HeartAccount;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.marketplace.BalanceLeaderboardEntry;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceAnalyticsService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.StatsLeaderboardEntry;
import com.magicstudios.magiccore.modules.statistics.StatsMetric;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CachedCompetitiveLeaderboards implements CompetitiveLeaderboardService {
 private final PlayerStatsService stats;private final LifestealService lifesteal;private final MarketplaceAnalyticsService market;private final Clock clock;private final Duration ttl;private final Map<String,Entry<?>>cache=new ConcurrentHashMap<>();
 public CachedCompetitiveLeaderboards(PlayerStatsService stats,LifestealService lifesteal,MarketplaceAnalyticsService market,Clock clock,Duration ttl){this.stats=stats;this.lifesteal=lifesteal;this.market=market;this.clock=clock;this.ttl=ttl;}
 @Override public CompletionStage<List<StatsLeaderboardEntry>>stats(StatsMetric metric,int limit){return cached("stats:"+metric+":"+limit,()->stats.leaderboard(metric,limit));}
 @Override public CompletionStage<List<HeartAccount>>hearts(int limit){return cached("hearts:"+limit,()->lifesteal.leaderboard(limit));}
 @Override public CompletionStage<List<BalanceLeaderboardEntry>>wealth(String currency,int limit){return cached("wealth:"+currency+":"+limit,()->market.balanceLeaderboard(currency,limit));}
 @Override public void invalidateStats(){cache.keySet().removeIf(key->key.startsWith("stats:"));}@Override public void invalidateHearts(){cache.keySet().removeIf(key->key.startsWith("hearts:"));}@Override public void invalidateWealth(){cache.keySet().removeIf(key->key.startsWith("wealth:"));}
 @SuppressWarnings("unchecked")private<T>CompletionStage<List<T>>cached(String key,Supplier<CompletionStage<List<T>>>loader){Entry<?>current=cache.get(key);if(current!=null&&current.expiresAt.isAfter(clock.instant()))return java.util.concurrent.CompletableFuture.completedFuture((List<T>)current.values);
  return loader.get().thenApply(values->{List<T>copy=List.copyOf(values);cache.put(key,new Entry<>(copy,clock.instant().plus(ttl)));return copy;});}
 private record Entry<T>(List<T>values,Instant expiresAt){}
}
