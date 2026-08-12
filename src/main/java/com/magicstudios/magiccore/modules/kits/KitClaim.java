package com.magicstudios.magiccore.modules.kits;

import java.time.Instant;
import java.util.UUID;

public record KitClaim(UUID playerId, String kitId, Instant claimedAt, Instant nextAvailableAt) {
}
