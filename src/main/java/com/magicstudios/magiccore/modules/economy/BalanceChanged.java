package com.magicstudios.magiccore.modules.economy;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BalanceChanged(UUID playerId, String currency, long beforeMinor, long afterMinor,
                             String operationKey, Instant occurredAt) implements DomainEvent {
}
