package com.magicstudios.magiccore.config.model;

import java.util.List;

public record SpawnStashFile(int configVersion, boolean observeOnly, long expirySeconds,
                             Placement placement, SignalPolicy signals, AlertPolicy alerts,
                             List<DecoyBlock> decoyBlocks) {
    public SpawnStashFile { decoyBlocks = List.copyOf(decoyBlocks); }

    public record Placement(int blockCount, int minimumHorizontalRadius, int maximumHorizontalRadius,
                            int minimumVerticalOffset, int maximumVerticalOffset,
                            int clusterRadius, int maximumCandidateAttempts) { }
    public record SignalPolicy(double approachDistance, double revealDistance, double revealDotProduct,
                               double suspiciousPathImprovement, int suspiciousPathSamples,
                               long perSignalCooldownSeconds) { }
    public record AlertPolicy(boolean enabled, String capability) { }
    public record DecoyBlock(String id, String blockData, int weight, List<LootAppearance> lootAppearance) {
        public DecoyBlock { lootAppearance = List.copyOf(lootAppearance); }
    }
    public record LootAppearance(String material, int minimumAmount, int maximumAmount, int weight) { }
}
