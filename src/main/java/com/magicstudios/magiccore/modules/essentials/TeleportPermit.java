package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.UUID;

public record TeleportPermit(UUID id, UUID playerId, String operationKey, long costMinor,
                             Instant reservedAt, Instant previousCooldownUntil, Status status) {
    public enum Status { RESERVED, COMPLETED, REFUNDED }
}
