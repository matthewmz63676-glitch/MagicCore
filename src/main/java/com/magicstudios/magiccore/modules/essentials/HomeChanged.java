package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record HomeChanged(UUID ownerId, String homeId, String change,
                          String operationKey, Instant occurredAt) implements DomainEvent {
}
