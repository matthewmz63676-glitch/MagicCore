package com.magicstudios.magiccore.config.model;

import com.magicstudios.magiccore.ranks.RankDefinition;
import com.magicstudios.magiccore.ranks.RankType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RanksFile(int configVersion, SystemSettings system, Map<String, RankEntry> ranks,
                        AdvancedPermissions advancedPermissions) {
    public RanksFile {
        ranks = Map.copyOf(ranks);
    }

    public Map<String, RankDefinition> definitions() {
        Map<String, RankDefinition> result = new LinkedHashMap<>();
        ranks.forEach((id, entry) -> result.put(id, new RankDefinition(id, entry.type(), entry.display(), entry.weight(),
                java.util.Set.copyOf(entry.inherits()), java.util.Set.copyOf(entry.perks()),
                java.util.Set.copyOf(entry.abilities()), entry.limits())));
        return Map.copyOf(result);
    }

    public record SystemSettings(String provider, String defaultRank, boolean syncToLuckperms) { }
    public record RankEntry(RankType type, String display, int weight, List<String> inherits,
                            List<String> perks, List<String> abilities, Map<String, Integer> limits) {
        public RankEntry {
            inherits = inherits == null ? List.of() : List.copyOf(inherits);
            perks = perks == null ? List.of() : List.copyOf(perks);
            abilities = abilities == null ? List.of() : List.copyOf(abilities);
            limits = limits == null ? Map.of() : Map.copyOf(limits);
        }
    }
    public record AdvancedPermissions(boolean enabled, Map<String, String> extraNodes) { }
}
