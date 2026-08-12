package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.Objects;

public record ServerWarp(String id, String displayName, WorldPosition position,
                         WarpAccess access, Instant updatedAt, String updatedBy) {
    public ServerWarp {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        position = Objects.requireNonNull(position, "position");
        access = Objects.requireNonNull(access, "access");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        updatedBy = Objects.requireNonNull(updatedBy, "updatedBy");
    }
}
