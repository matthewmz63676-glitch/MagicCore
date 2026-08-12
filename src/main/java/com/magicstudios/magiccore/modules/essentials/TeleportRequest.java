package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.UUID;

public record TeleportRequest(UUID requesterId, UUID targetId, Direction direction, Instant createdAt, Instant expiresAt) {
    public enum Direction { REQUESTER_TO_TARGET, TARGET_TO_REQUESTER }
}
