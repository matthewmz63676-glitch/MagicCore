package com.magicstudios.magiccore.modules.essentials;

import java.util.Objects;
import java.util.UUID;

public record WorldPosition(UUID worldId, String worldName, double x, double y, double z,
                            float yaw, float pitch) {
    public WorldPosition {
        worldId = Objects.requireNonNull(worldId, "worldId");
        worldName = Objects.requireNonNull(worldName, "worldName");
        if (worldName.isBlank()) throw new IllegalArgumentException("worldName must not be blank");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Coordinates must be finite");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) throw new IllegalArgumentException("Rotation must be finite");
    }

    public double distanceSquared(WorldPosition other) {
        if (!worldId.equals(other.worldId)) return Double.POSITIVE_INFINITY;
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
