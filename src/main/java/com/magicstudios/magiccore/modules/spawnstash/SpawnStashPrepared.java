package com.magicstudios.magiccore.modules.spawnstash;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SpawnStashPrepared(UUID caseId, UUID targetId, UUID actorId, int blockCount,
                                 Instant occurredAt) implements DomainEvent { }
