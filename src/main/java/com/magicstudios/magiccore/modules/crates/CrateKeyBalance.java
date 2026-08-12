package com.magicstudios.magiccore.modules.crates;

import java.time.Instant;
import java.util.UUID;

public record CrateKeyBalance(UUID playerId, String keyId, long amount, Instant updatedAt) { }
