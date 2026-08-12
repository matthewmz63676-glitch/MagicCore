package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.UUID;

public record BackLocations(UUID playerId, WorldPosition previousTeleport,
                            WorldPosition lastDeath, Instant updatedAt) {
}
