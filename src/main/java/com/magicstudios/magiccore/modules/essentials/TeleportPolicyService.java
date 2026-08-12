package com.magicstudios.magiccore.modules.essentials;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface TeleportPolicyService {
    CompletionStage<TeleportPermit> reserve(UUID playerId, String operationKey);
    CompletionStage<Boolean> complete(TeleportPermit permit);
    CompletionStage<Boolean> refund(TeleportPermit permit, String reason);
}
