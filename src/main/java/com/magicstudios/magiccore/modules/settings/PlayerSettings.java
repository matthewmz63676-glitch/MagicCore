package com.magicstudios.magiccore.modules.settings;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlayerSettings(UUID playerId, Map<PlayerSetting, Boolean> values, Instant updatedAt) {
    public PlayerSettings {
        values = Map.copyOf(values);
    }

    public boolean enabled(PlayerSetting setting) {
        return values.getOrDefault(setting, setting.defaultValue());
    }
}
