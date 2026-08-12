package com.magicstudios.magiccore.delivery;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record MailboxDelivery(UUID id, UUID recipientId, String operationKey, String payloadType,
                              String payloadBase64, DeliveryStatus status, Instant createdAt,
                              Instant deliveredAt) {
    public MailboxDelivery {
        id = Objects.requireNonNull(id, "id");
        recipientId = Objects.requireNonNull(recipientId, "recipientId");
        operationKey = Objects.requireNonNull(operationKey, "operationKey");
        payloadType = Objects.requireNonNull(payloadType, "payloadType");
        payloadBase64 = Objects.requireNonNull(payloadBase64, "payloadBase64");
        status = Objects.requireNonNull(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static MailboxDelivery pending(UUID id, UUID recipientId, String operationKey,
                                          String payloadType, byte[] payload, Instant now) {
        return new MailboxDelivery(id, recipientId, operationKey, payloadType,
                Base64.getEncoder().encodeToString(payload), DeliveryStatus.PENDING, now, null);
    }

    public byte[] payload() {
        return Base64.getDecoder().decode(payloadBase64);
    }
}
