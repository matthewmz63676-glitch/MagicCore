package com.magicstudios.magiccore.modules.teams;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Team(UUID id, String name, String normalizedName, UUID ownerId,
                   Map<UUID, TeamRole> members, Instant createdAt, long revision) {
    public Team {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        normalizedName = Objects.requireNonNull(normalizedName, "normalizedName");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        members = Map.copyOf(members);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (members.get(ownerId) != TeamRole.LEADER) throw new IllegalArgumentException("Team owner must be its leader");
    }
}
