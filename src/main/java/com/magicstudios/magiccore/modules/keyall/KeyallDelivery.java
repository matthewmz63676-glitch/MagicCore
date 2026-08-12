package com.magicstudios.magiccore.modules.keyall;

import java.time.Instant;
import java.util.UUID;

public record KeyallDelivery(UUID runId, UUID playerId, String keyId, long amount, Status status,
                             String detail, Instant updatedAt) {
    public enum Status { DELIVERED, FAILED }
}
