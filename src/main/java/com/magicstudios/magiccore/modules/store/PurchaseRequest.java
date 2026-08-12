package com.magicstudios.magiccore.modules.store;

import java.time.Instant;
import java.util.UUID;

public record PurchaseRequest(String eventId, String productId, UUID playerId, String playerName,
                              long paidMinor, Instant occurredAt, String nonce) {
    public PurchaseRequest { if (eventId.isBlank() || productId.isBlank() || nonce.isBlank()) throw new IllegalArgumentException("purchase identity is required"); }
}
