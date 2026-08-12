package com.magicstudios.magiccore.ranks;

import java.util.Set;

public record RankSyncPreview(String playerId, String fromGroup, String toGroup,
                              Set<String> additions, Set<String> removals,
                              Set<String> preservedUnrelatedNodes) {
    public RankSyncPreview {
        additions = Set.copyOf(additions);
        removals = Set.copyOf(removals);
        preservedUnrelatedNodes = Set.copyOf(preservedUnrelatedNodes);
    }
}
