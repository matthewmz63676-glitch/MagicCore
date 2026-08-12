package com.magicstudios.magiccore.modules.store;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PurchaseFulfilled(String eventId, String productId, UUID playerId, String playerName,
                                long paidMinor, Instant occurredAt) implements DomainEvent { }
