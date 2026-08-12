package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Home(UUID ownerId, String id, String displayName, WorldPosition position,
                   Set<UUID> sharedWith, Instant createdAt, Instant updatedAt) {
    public Home {
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        position = Objects.requireNonNull(position, "position");
        sharedWith = Set.copyOf(sharedWith);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public boolean visibleTo(UUID viewerId) {
        return ownerId.equals(viewerId) || sharedWith.contains(viewerId);
    }
}
