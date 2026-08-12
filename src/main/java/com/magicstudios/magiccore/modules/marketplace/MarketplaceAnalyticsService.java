package com.magicstudios.magiccore.modules.marketplace;
import java.util.List;
import java.util.concurrent.CompletionStage;
public interface MarketplaceAnalyticsService{
 CompletionStage<MarketplaceSnapshot> snapshot();
 CompletionStage<List<BalanceLeaderboardEntry>> balanceLeaderboard(String currency,int limit);
}
