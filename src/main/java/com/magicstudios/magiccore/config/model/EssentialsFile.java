package com.magicstudios.magiccore.config.model;

import java.util.List;

public record EssentialsFile(int configVersion, Homes homes, Teleport teleport, Rtp rtp, List<Kit> kits) {
    public EssentialsFile { kits = List.copyOf(kits); }
    public record Homes(int maximumNameLength) { }
    public record Teleport(long requestLifetimeSeconds, long warmupSeconds, double movementTolerance,
                           long cooldownSeconds, long costMinor, String currency) { }
    public record Rtp(double centerX, double centerZ, double minimumRadius, double maximumRadius, int maximumAttempts) { }
    public record Kit(String id, String displayName, long cooldownSeconds, String capability, List<Item> items) {
        public Kit { items = List.copyOf(items); }
    }
    public record Item(String material, int amount, String itemDataBase64) { }
}
