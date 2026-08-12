package com.magicstudios.magiccore.modules.spawnstash;

import java.time.Instant;
import java.util.UUID;

public record SpawnStashNote(UUID id, UUID actorId, String actorName, String text, Instant createdAt) { }
