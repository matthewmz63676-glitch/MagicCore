package com.magicstudios.magiccore.modules.statistics;

import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface PlayerStatsService {
    CompletionStage<PlayerStats> stats(UUID playerId);
    CompletionStage<PlayerStats> recordKill(VerifiedPlayerKill kill, String operationKey);
    CompletionStage<PlayerStats> recordDeath(UUID playerId, UUID deathEventId, String operationKey);
    CompletionStage<PlayerStats> addPlaytime(UUID playerId, long seconds, String operationKey);
    CompletionStage<List<StatsLeaderboardEntry>> leaderboard(StatsMetric metric, int limit);
}
