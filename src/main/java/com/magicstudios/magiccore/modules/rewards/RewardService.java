package com.magicstudios.magiccore.modules.rewards;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface RewardService {
    List<RewardDefinition> dailyPool();

    List<PlaytimeMilestone> milestones();

    CompletionStage<RewardClaimResult> claimDaily(UUID playerId, String operationKey);

    CompletionStage<RewardClaimResult> claimPlaytime(UUID playerId, long authoritativePlaytimeMinutes,
                                                     String milestoneId, String operationKey);

    CompletionStage<DailyRewardState> dailyState(UUID playerId);

    CompletionStage<PlaytimeRewardState> playtimeState(UUID playerId);
}
