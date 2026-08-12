package com.magicstudios.magiccore.modules.rewards;

import java.util.Set;
import java.util.UUID;

public record PlaytimeRewardState(UUID playerId, Set<String> claimedMilestones) {
    public PlaytimeRewardState {
        claimedMilestones = Set.copyOf(claimedMilestones);
    }
}
