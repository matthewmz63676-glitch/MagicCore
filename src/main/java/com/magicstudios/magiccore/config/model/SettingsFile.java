package com.magicstudios.magiccore.config.model;

import java.util.Map;

public record SettingsFile(int configVersion, Map<String, Boolean> defaults) {
    public SettingsFile { defaults = Map.copyOf(defaults); }
}
