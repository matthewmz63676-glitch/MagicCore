package com.magicstudios.magiccore.ranks;

import java.time.Instant;
import java.util.UUID;

public record RankMembership(UUID playerId, String rankId, Instant updatedAt, String updatedBy) {
}
