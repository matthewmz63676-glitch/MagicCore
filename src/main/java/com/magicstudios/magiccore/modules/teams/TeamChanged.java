package com.magicstudios.magiccore.modules.teams;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TeamChanged(UUID teamId, String change, UUID actorId, Instant occurredAt) implements DomainEvent {
}
