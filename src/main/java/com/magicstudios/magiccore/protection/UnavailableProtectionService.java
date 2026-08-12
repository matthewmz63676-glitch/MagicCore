package com.magicstudios.magiccore.protection;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Explicit fail-closed mode for configurations that require a missing protection provider. */
public final class UnavailableProtectionService implements ProtectionService {
    private final String provider;
    public UnavailableProtectionService(String provider) { this.provider = provider; }
    @Override public CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action) {
        return CompletableFuture.completedFuture(ProtectionDecision.deny(provider, "PROVIDER_UNAVAILABLE"));
    }
    @Override public String providerId() { return provider; }
}
