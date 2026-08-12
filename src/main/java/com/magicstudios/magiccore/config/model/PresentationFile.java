package com.magicstudios.magiccore.config.model;

import java.util.List;

/** Configuration-only content consumed by menu and command view adapters. */
public record PresentationFile(int configVersion, List<NavigationEntry> info,
                               List<NavigationEntry> serverNavigation,
                               Applications applications) {
    public PresentationFile {
        info = List.copyOf(info);
        serverNavigation = List.copyOf(serverNavigation);
    }

    public record NavigationEntry(String id, String title, String description,
                                  String material, int slot, String action,
                                  String requiredCapability) { }

    public record Applications(ApplicationDefinition media,
                               ApplicationDefinition staff) { }

    public record ApplicationDefinition(String title, String applyUrl,
                                        List<Requirement> requirements) {
        public ApplicationDefinition {
            requirements = List.copyOf(requirements);
        }
    }

    public record Requirement(String id, String type, String label,
                              long target, String capability) { }
}
