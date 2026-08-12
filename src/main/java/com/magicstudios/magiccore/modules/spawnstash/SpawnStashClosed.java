package com.magicstudios.magiccore.modules.spawnstash;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SpawnStashClosed(UUID caseId, SpawnStashCase.Outcome outcome,
                               Instant occurredAt) implements DomainEvent { }
