package com.magicstudios.magiccore.modules.playerwarps;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import java.time.Instant;
import java.util.UUID;

public record PlayerWarp(String id, String displayName, UUID ownerId, WorldPosition position, String category,
                         Status status, long visits, Instant createdAt, Instant updatedAt, Instant expiresAt) {
    public enum Status { ACTIVE, PENDING_REVIEW, SUSPENDED }
    public boolean activeAt(Instant now) { return status == Status.ACTIVE && (expiresAt == null || expiresAt.isAfter(now)); }
}
