package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public final class BukkitWorldPositions {
    private BukkitWorldPositions() { }
    public static WorldPosition from(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new WorldPosition(world.getUID(), world.getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }
}
