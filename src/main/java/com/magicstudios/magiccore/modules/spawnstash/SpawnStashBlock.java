package com.magicstudios.magiccore.modules.spawnstash;

import java.util.List;

public record SpawnStashBlock(StashPosition position, String originalBlockData,
                              String decoyBlockData, List<LootAppearance> lootAppearance,
                              State state) {
    public SpawnStashBlock { lootAppearance = List.copyOf(lootAppearance); }
    public enum State { PLANNED, PLACED, RESTORED }
    public record LootAppearance(String material, int amount) { }
}
