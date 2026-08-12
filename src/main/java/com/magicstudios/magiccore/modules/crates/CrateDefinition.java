package com.magicstudios.magiccore.modules.crates;

import java.util.List;

public record CrateDefinition(String id, String displayName, CrateCost cost, int maximumOpenAmount,
                              List<CrateReward> rewards, List<Milestone> milestones) {
    public CrateDefinition { rewards = List.copyOf(rewards); milestones = List.copyOf(milestones); }
    public record Milestone(long openCount, String rewardId) { }
}
