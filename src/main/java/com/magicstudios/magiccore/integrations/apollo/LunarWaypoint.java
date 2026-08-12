package com.magicstudios.magiccore.integrations.apollo;

public record LunarWaypoint(String name, String world, int x, int y, int z,
                            int rgb, boolean preventRemoval) {
    public LunarWaypoint {
        if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("waypoint name must be 1-64 characters");
        if (world == null || world.isBlank()) throw new IllegalArgumentException("world is required");
        rgb &= 0xFFFFFF;
    }
}
