package com.magicstudios.magiccore.modules.crates;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CrateKeysChanged(UUID playerId, String keyId, long balance,
                               String operationKey, Instant occurredAt) implements DomainEvent { }
