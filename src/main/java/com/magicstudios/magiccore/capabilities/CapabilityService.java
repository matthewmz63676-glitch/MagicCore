package com.magicstudios.magiccore.capabilities;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CapabilityService {
    CompletionStage<Boolean> has(UUID playerId, String capability);

    CompletionStage<Integer> limit(UUID playerId, String limitId);

    CompletionStage<Boolean> canTarget(UUID actorId, UUID targetId);
}
