package com.magicstudios.magiccore.protection;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Explicit NONE provider. This is never used as a silent fallback for a configured integration. */
public final class AllowAllProtectionService implements ProtectionService {
    @Override public CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action) {
        return CompletableFuture.completedFuture(ProtectionDecision.allow("NONE"));
    }
    @Override public String providerId() { return "NONE"; }
}
