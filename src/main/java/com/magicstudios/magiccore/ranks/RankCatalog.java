package com.magicstudios.magiccore.ranks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RankCatalog {
    private final String defaultRank;
    private final Map<String, RankDefinition> definitions;

    public RankCatalog(String defaultRank, Map<String, RankDefinition> definitions) {
        this.defaultRank = defaultRank;
        this.definitions = Map.copyOf(definitions);
        validate();
    }

    public String defaultRank() {
        return defaultRank;
    }

    public Map<String, RankDefinition> definitions() {
        return definitions;
    }

    public RankDefinition require(String id) {
        RankDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown rank ID: " + id);
        return definition;
    }

    public Set<String> capabilities(String id) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectCapabilities(id, result, new HashSet<>());
        return Set.copyOf(result);
    }

    public int limit(String id, String limitId) {
        return collectLimit(id, limitId, new HashSet<>());
    }

    private void collectCapabilities(String id, Set<String> result, Set<String> visited) {
        if (!visited.add(id)) return;
        RankDefinition definition = require(id);
        definition.inherits().forEach(parent -> collectCapabilities(parent, result, visited));
        result.addAll(definition.perks());
        result.addAll(definition.abilities());
    }

    private int collectLimit(String id, String limitId, Set<String> visited) {
        if (!visited.add(id)) return 0;
        RankDefinition definition = require(id);
        int inherited = definition.inherits().stream().mapToInt(parent -> collectLimit(parent, limitId, visited)).max().orElse(0);
        return Math.max(inherited, definition.limits().getOrDefault(limitId, 0));
    }

    private void validate() {
        require(defaultRank);
        long donors = definitions.values().stream().filter(rank -> rank.type() == RankType.DONOR).count();
        long staff = definitions.values().stream().filter(rank -> rank.type() == RankType.STAFF).count();
        if (donors != 5 || staff != 5) {
            throw new IllegalArgumentException("ranks.yml must define exactly five DONOR and five STAFF examples");
        }
        for (RankDefinition rank : definitions.values()) {
            for (String parent : rank.inherits()) require(parent);
        }
        Map<String, Visit> visits = new HashMap<>();
        Deque<String> path = new ArrayDeque<>();
        for (String id : definitions.keySet()) validateCycle(id, visits, path);
    }

    private void validateCycle(String id, Map<String, Visit> visits, Deque<String> path) {
        if (visits.get(id) == Visit.COMPLETE) return;
        if (visits.get(id) == Visit.ACTIVE) {
            throw new IllegalArgumentException("Rank inheritance cycle: " + String.join(" -> ", path) + " -> " + id);
        }
        visits.put(id, Visit.ACTIVE);
        path.addLast(id);
        require(id).inherits().forEach(parent -> validateCycle(parent, visits, path));
        path.removeLast();
        visits.put(id, Visit.COMPLETE);
    }

    private enum Visit { ACTIVE, COMPLETE }
}
