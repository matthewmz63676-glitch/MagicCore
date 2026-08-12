package com.magicstudios.magiccore.modules.essentials;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface BackService {
    CompletionStage<Void> recordTeleportOrigin(UUID playerId, WorldPosition origin, String operationKey);

    CompletionStage<Void> recordDeath(UUID playerId, WorldPosition deathLocation, String operationKey);

    CompletionStage<Optional<WorldPosition>> previousTeleport(UUID playerId);

    CompletionStage<Optional<WorldPosition>> lastDeath(UUID playerId);
}
