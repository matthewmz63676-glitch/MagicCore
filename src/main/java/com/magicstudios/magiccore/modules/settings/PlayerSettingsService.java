package com.magicstudios.magiccore.modules.settings;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface PlayerSettingsService {
    CompletionStage<PlayerSettings> get(UUID playerId);
    CompletionStage<PlayerSettings> set(UUID playerId, PlayerSetting setting, boolean value, String operationKey);
}
