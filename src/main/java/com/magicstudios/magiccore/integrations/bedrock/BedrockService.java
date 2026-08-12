package com.magicstudios.magiccore.integrations.bedrock;

import java.util.UUID;

public interface BedrockService {
    boolean available();
    boolean isBedrockPlayer(UUID playerId);
    boolean useBedrockSafeInteractions();
}
