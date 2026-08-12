package com.magicstudios.magiccore.modules.store;

import java.time.Instant;
import java.util.UUID;

public record PurchaseRecord(String eventId, String productId, UUID playerId, String playerName, long paidMinor,
                             int nextAction, Status status, Instant receivedAt, Instant completedAt) {
    public enum Status { PROCESSING, COMPLETE }
}
