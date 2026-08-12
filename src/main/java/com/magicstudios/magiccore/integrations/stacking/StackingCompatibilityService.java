package com.magicstudios.magiccore.integrations.stacking;

import org.bukkit.block.Block;

public interface StackingCompatibilityService {
    String provider();
    boolean available();
    boolean isManagedSpawner(Block block);
}
