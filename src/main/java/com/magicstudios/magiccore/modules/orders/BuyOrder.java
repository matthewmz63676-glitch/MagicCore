package com.magicstudios.magiccore.modules.orders;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import java.time.Instant;
import java.util.UUID;

public record BuyOrder(UUID id, UUID buyerId, String category, ItemFingerprint fingerprint,
                       int requestedQuantity, int filledQuantity, int reservedQuantity,
                       String currency, long unitPriceMinor, long escrowRemainingMinor,
                       Status status, Instant createdAt, Instant expiresAt, Instant closedAt) {
    public enum Status { OPEN, FILLED, CANCELLED, EXPIRED }
    public int availableQuantity() { return requestedQuantity - filledQuantity - reservedQuantity; }
    public boolean openAt(Instant now) { return status == Status.OPEN && expiresAt.isAfter(now); }
}
