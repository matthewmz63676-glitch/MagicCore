package com.magicstudios.magiccore.modules.auction;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import java.time.Instant;
import java.util.UUID;

public record AuctionListing(UUID id, UUID sellerId, UUID buyerId, String category,
                             ItemFingerprint fingerprint, int quantity, String itemPayloadBase64,
                             String currency, long priceMinor, long listingFeeMinor, Status status,
                             Instant createdAt, Instant activeAt, Instant expiresAt, Instant closedAt,
                             String closeOperationKey) {
    public enum Status { PREPARING, ACTIVE, SOLD, CANCELLED, EXPIRED, REJECTED }
    public boolean purchasableAt(Instant now) { return status == Status.ACTIVE && expiresAt.isAfter(now); }
}
