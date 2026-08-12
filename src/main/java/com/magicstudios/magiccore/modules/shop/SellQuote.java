package com.magicstudios.magiccore.modules.shop;

import java.time.Instant;
import java.util.UUID;

public record SellQuote(UUID id, UUID playerId, String productId, ItemFingerprint fingerprint,
                        int itemQuantity, long creditMinor, Status status, String executionKey,
                        String recoveryPayloadBase64, Instant createdAt, Instant expiresAt, Instant updatedAt) {
    public enum Status { QUOTED, REMOVING, REMOVED, SETTLED, REJECTED, RECOVERY_REQUIRED }
}
