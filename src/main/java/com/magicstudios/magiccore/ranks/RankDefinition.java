package com.magicstudios.magiccore.ranks;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RankDefinition(String id, RankType type, String display, int weight,
                             Set<String> inherits, Set<String> perks,
                             Set<String> abilities, Map<String, Integer> limits) {
    public RankDefinition {
        id = Objects.requireNonNull(id, "id");
        if (!id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Rank ID must be upper snake case");
        type = Objects.requireNonNull(type, "type");
        display = Objects.requireNonNull(display, "display");
        inherits = Set.copyOf(inherits);
        perks = Set.copyOf(perks);
        abilities = Set.copyOf(abilities);
        limits = Map.copyOf(limits);
        if (limits.values().stream().anyMatch(value -> value < 0)) throw new IllegalArgumentException("Rank limits cannot be negative");
    }
}
