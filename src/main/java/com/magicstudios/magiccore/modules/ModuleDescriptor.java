package com.magicstudios.magiccore.modules;

import com.magicstudios.magiccore.api.ProviderMode;

import java.util.Objects;
import java.util.Set;

public record ModuleDescriptor(String id, ProviderMode mode, Set<String> requiredModules) {
    public ModuleDescriptor {
        Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("Module ID must be lower kebab-case: " + id);
        }
        mode = Objects.requireNonNull(mode, "mode");
        requiredModules = Set.copyOf(requiredModules);
        if (requiredModules.contains(id)) {
            throw new IllegalArgumentException("Module cannot depend on itself: " + id);
        }
    }
}
