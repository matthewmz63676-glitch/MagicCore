package com.magicstudios.magiccore.modules.auction;

import java.time.Instant;
import java.util.UUID;

public record AuctionCompleted(UUID listingId, UUID sellerId, UUID buyerId, long priceMinor,
                               String currency, String operationKey, Instant occurredAt)
        implements com.magicstudios.magiccore.api.DomainEvent { }
