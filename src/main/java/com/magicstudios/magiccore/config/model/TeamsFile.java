package com.magicstudios.magiccore.config.model;

import java.util.Set;

public record TeamsFile(int configVersion, NamePolicy namePolicy, Invitations invitations,
                        boolean friendlyFire) {
    public record NamePolicy(int minimumLength, int maximumLength, String pattern, Set<String> blockedNames) {
        public NamePolicy {
            blockedNames = Set.copyOf(blockedNames);
        }
    }
    public record Invitations(long expiresSeconds) { }
}
