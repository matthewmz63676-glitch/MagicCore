package com.magicstudios.magiccore.integrations.discord;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface DiscordIntegrationService {
    String provider();
    boolean available();
    CompletionStage<Optional<String>> linkedDiscordId(UUID playerId);
    CompletionStage<Boolean> notify(String message);
}
