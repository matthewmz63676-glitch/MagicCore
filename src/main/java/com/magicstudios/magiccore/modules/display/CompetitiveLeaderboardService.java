package com.magicstudios.magiccore.modules.display;

import com.magicstudios.magiccore.modules.lifesteal.HeartAccount;
import com.magicstudios.magiccore.modules.marketplace.BalanceLeaderboardEntry;
import com.magicstudios.magiccore.modules.statistics.StatsLeaderboardEntry;
import com.magicstudios.magiccore.modules.statistics.StatsMetric;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CompetitiveLeaderboardService {
 CompletionStage<List<StatsLeaderboardEntry>>stats(StatsMetric metric,int limit);
 CompletionStage<List<HeartAccount>>hearts(int limit);
 CompletionStage<List<BalanceLeaderboardEntry>>wealth(String currency,int limit);
 void invalidateStats();void invalidateHearts();void invalidateWealth();
}
