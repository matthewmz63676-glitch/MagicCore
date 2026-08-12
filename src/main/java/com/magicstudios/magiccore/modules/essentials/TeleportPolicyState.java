package com.magicstudios.magiccore.modules.essentials;

import java.time.Instant;
import java.util.UUID;

public record TeleportPolicyState(UUID playerId, UUID activePermitId, Instant cooldownUntil, Instant updatedAt) { }
