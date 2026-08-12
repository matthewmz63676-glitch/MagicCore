package com.magicstudios.magiccore.modules.profiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface PlayerProfileService {
    CompletionStage<PlayerProfile> recordSeen(UUID playerId, String currentName, String locale, Instant seenAt);

    CompletionStage<Optional<PlayerProfile>> find(UUID playerId);

    default CompletionStage<Optional<PlayerProfile>> findByCurrentName(String currentName) {
        return java.util.concurrent.CompletableFuture.completedFuture(Optional.empty());
    }

    CompletionStage<PlayerProfile> setLocale(UUID playerId, String locale, String operationKey);

    CompletionStage<PlayerProfile> setSetting(UUID playerId, String key, String value, String operationKey);
}
