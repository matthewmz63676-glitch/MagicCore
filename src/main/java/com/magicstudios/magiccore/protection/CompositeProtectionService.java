package com.magicstudios.magiccore.protection;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class CompositeProtectionService implements ProtectionService {
    private final List<ProtectionService> providers;
    public CompositeProtectionService(List<ProtectionService> providers) { this.providers = List.copyOf(providers); }
    @Override public CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action) {
        List<CompletableFuture<ProtectionDecision>> checks = providers.stream()
                .map(provider -> provider.check(playerId, position, action).toCompletableFuture()).toList();
        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(ignored -> checks.stream()
                .map(CompletableFuture::join).filter(decision -> !decision.allowed()).findFirst()
                .orElseGet(() -> ProtectionDecision.allow(providerId())));
    }
    @Override public String providerId() { return providers.stream().map(ProtectionService::providerId).reduce((a, b) -> a + "+" + b).orElse("NONE"); }
}
