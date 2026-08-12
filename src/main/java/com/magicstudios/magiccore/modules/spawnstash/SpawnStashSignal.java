package com.magicstudios.magiccore.modules.spawnstash;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SpawnStashSignal(UUID id, Type type, UUID playerId, StashPosition position,
                               Map<String, String> details, Instant occurredAt) {
    public SpawnStashSignal { details = Map.copyOf(details); }
    public enum Type { REVEAL, APPROACH, INTERACT, BREAK, SUSPICIOUS_PATH, VULCAN_FLAG }
}
