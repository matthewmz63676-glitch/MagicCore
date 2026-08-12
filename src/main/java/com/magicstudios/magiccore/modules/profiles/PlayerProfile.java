package com.magicstudios.magiccore.modules.profiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PlayerProfile(UUID playerId, List<String> knownNames, String locale,
                            Instant firstSeen, Instant lastSeen, Map<String, String> settings) {
    public PlayerProfile {
        playerId = Objects.requireNonNull(playerId, "playerId");
        knownNames = List.copyOf(knownNames);
        if (knownNames.isEmpty()) {
            throw new IllegalArgumentException("knownNames must contain at least one name");
        }
        locale = Objects.requireNonNull(locale, "locale");
        firstSeen = Objects.requireNonNull(firstSeen, "firstSeen");
        lastSeen = Objects.requireNonNull(lastSeen, "lastSeen");
        settings = Map.copyOf(settings);
    }

    public String currentName() {
        return knownNames.get(knownNames.size() - 1);
    }
}
