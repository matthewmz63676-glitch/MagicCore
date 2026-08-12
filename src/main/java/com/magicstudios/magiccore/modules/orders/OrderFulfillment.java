package com.magicstudios.magiccore.modules.orders;

import java.time.Instant;
import java.util.UUID;

public record OrderFulfillment(UUID id, UUID orderId, UUID sellerId, int quantity, long payoutMinor,
                               Status status, String itemPayloadBase64, String operationKey,
                               Instant createdAt, Instant settledAt) {
    public enum Status { PREPARING, SETTLED, REJECTED }
}
