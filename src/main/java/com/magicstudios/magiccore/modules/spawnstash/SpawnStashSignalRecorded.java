package com.magicstudios.magiccore.modules.spawnstash;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SpawnStashSignalRecorded(UUID caseId, UUID targetId, SpawnStashSignal signal,
                                       Instant occurredAt) implements DomainEvent { }
