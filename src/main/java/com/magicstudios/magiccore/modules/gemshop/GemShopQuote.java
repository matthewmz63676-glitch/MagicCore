package com.magicstudios.magiccore.modules.gemshop;

import java.time.Instant;
import java.util.UUID;

public record GemShopQuote(UUID id, UUID playerId, GemProduct product, Status status,
                           Instant createdAt, Instant expiresAt) {
    public enum Status { QUOTED, CONFIRMED, EXPIRED }
}
