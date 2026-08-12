package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.api.ProviderMode;

import java.util.Map;
import java.util.Objects;

public record SetupPlan(SetupPreset preset, String storageProvider,
                        Map<String, ProviderMode> featureProviders,
                        Map<String, Boolean> integrations, boolean reviewed) {
    public SetupPlan {
        preset = Objects.requireNonNull(preset, "preset");
        storageProvider = Objects.requireNonNull(storageProvider, "storageProvider");
        featureProviders = Map.copyOf(featureProviders);
        integrations = Map.copyOf(integrations);
    }

    public SetupPlan markReviewed() {
        return new SetupPlan(preset, storageProvider, featureProviders, integrations, true);
    }
}
