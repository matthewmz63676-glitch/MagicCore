package com.magicstudios.magiccore.modules.spawnstash;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpawnStashCase(UUID id, UUID targetId, UUID actorId, String actorName,
                             StashPosition origin, Status status, Outcome outcome,
                             boolean observeOnly, List<SpawnStashBlock> blocks,
                             List<SpawnStashSignal> signals, List<SpawnStashNote> notes,
                             Instant createdAt, Instant expiresAt, Instant updatedAt,
                             Instant closedAt) {
    public SpawnStashCase {
        blocks = List.copyOf(blocks); signals = List.copyOf(signals); notes = List.copyOf(notes);
    }
    public enum Status { PREPARED, ACTIVE, CLEANING, CLOSED }
    public enum Outcome { OPEN, NO_FINDING, CONFIRMED, FALSE_POSITIVE, CANCELLED, EXPIRED }
}
