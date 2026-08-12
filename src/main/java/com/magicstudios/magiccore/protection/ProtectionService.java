package com.magicstudios.magiccore.protection;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ProtectionService {
    CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action);
    String providerId();
}
