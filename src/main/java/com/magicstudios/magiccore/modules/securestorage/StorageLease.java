package com.magicstudios.magiccore.modules.securestorage;

import java.time.Instant;
import java.util.UUID;

public record StorageLease(UUID id, UUID actorId, UUID ownerId, VirtualContainer.Type type, int containerIndex,
                           int size, long expectedRevision, Status status, Instant openedAt, Instant expiresAt, Instant updatedAt) {
    public enum Status { OPEN, COMMITTED, CLOSED, RECOVERED }
}
