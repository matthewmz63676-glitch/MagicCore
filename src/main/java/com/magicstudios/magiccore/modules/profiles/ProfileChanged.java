package com.magicstudios.magiccore.modules.profiles;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProfileChanged(UUID playerId, String field, Instant occurredAt) implements DomainEvent {
}
