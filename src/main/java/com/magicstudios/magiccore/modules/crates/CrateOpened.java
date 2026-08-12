package com.magicstudios.magiccore.modules.crates;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CrateOpened(UUID playerId, String crateId, int amount, UUID openingId,
                          String operationKey, Instant occurredAt) implements DomainEvent { }
