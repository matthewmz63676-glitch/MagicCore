package com.magicstudios.magiccore.config.model;

import com.magicstudios.magiccore.modules.rewards.MilestonePolicy;
import com.magicstudios.magiccore.modules.rewards.PlaytimeMilestone;
import com.magicstudios.magiccore.modules.rewards.RewardDefinition;

import java.util.List;

public record RewardsFile(int configVersion, Daily daily, Playtime playtime) {
    public record Daily(long cooldownHours, int choices, List<DailyEntry> pool) {
        public Daily { pool = List.copyOf(pool); }
        public List<RewardDefinition> definitions() {
            return pool.stream().map(entry -> new RewardDefinition(entry.id(), entry.display(), entry.rarity(),
                    entry.weight(), entry.currency(), entry.amountMinor())).toList();
        }
    }
    public record DailyEntry(String id, String display, String rarity, int weight,
                             String currency, long amountMinor) { }
    public record Playtime(MilestonePolicy policy, List<MilestoneEntry> milestones) {
        public Playtime { milestones = List.copyOf(milestones); }
        public List<PlaytimeMilestone> definitions() {
            return milestones.stream().map(entry -> new PlaytimeMilestone(entry.id(), entry.display(),
                    entry.requiredMinutes(), entry.currency(), entry.amountMinor())).toList();
        }
    }
    public record MilestoneEntry(String id, String display, long requiredMinutes,
                                 String currency, long amountMinor) { }
}
