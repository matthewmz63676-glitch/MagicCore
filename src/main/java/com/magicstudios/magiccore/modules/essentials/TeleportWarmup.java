package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.UUID;

public record TeleportWarmup(UUID playerId, WorldPosition origin, WorldPosition destination,
                             Instant completesAt, String operationKey) {
}
