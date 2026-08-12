package com.magicstudios.magiccore.config.model;

import java.util.List;

public record ItemWorthFile(int configVersion, String currency, Policies policies,
                            Presentation presentation, List<WorthEntry> entries) {
    public ItemWorthFile { entries = List.copyOf(entries); }
    public record Policies(String enchantments, long enchantmentBasisPointsPerLevel,
                           String durability, long minimumDurabilityBasisPoints,
                           String metadata, String containers, String spawners,
                           List<String> protectedItemIds, List<String> protectedMetadataKeys) {
        public Policies { protectedItemIds=List.copyOf(protectedItemIds);protectedMetadataKeys=List.copyOf(protectedMetadataKeys); }
    }
    public record Presentation(boolean lore, boolean actionBar, boolean placeholder,
                               String worthTemplate, String unavailableText) { }
    public record WorthEntry(String id, String itemId, String category, long unitWorthMinor) { }
}
