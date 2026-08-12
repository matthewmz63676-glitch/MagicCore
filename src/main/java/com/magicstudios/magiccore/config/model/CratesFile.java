package com.magicstudios.magiccore.config.model;

import java.util.List;

public record CratesFile(int configVersion, String currency, List<Crate> crates, Keyall keyall) {
    public CratesFile { crates = List.copyOf(crates); }
    public record Crate(String id, String displayName, Cost cost, int maximumOpenAmount,
                        List<Reward> rewards, List<Milestone> milestones) {
        public Crate { rewards = List.copyOf(rewards); milestones = List.copyOf(milestones); }
    }
    public record Cost(String type, String keyId, long amount) { }
    public record Reward(String id, String type, long weight, String rarity, String material, int amount,
                         String itemDataBase64, String currency, long amountMinor, String keyId, long keyAmount) { }
    public record Milestone(long openCount, String rewardId) { }
    public record Keyall(boolean enabled, int maximumRecipients, List<KeyallDefinition> definitions) {
        public Keyall { definitions = List.copyOf(definitions); }
    }
    public record KeyallDefinition(String id, String keyId, long amount, String audience, boolean offlineDelivery,
                                   long scheduleIntervalSeconds, long threshold) { }
}
