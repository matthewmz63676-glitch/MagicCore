package com.magicstudios.magiccore.config.model;

import com.magicstudios.magiccore.api.ProviderMode;

import java.util.Map;

public record FeaturesFile(int configVersion, Map<String, ProviderMode> features) {
    public FeaturesFile {
        features = Map.copyOf(features);
    }
}
