package com.magicstudios.magiccore.modules.essentials;

import java.util.Set;

public record WarpAccess(boolean publicAccess, Set<String> ranks, Set<String> capabilities) {
    public WarpAccess {
        ranks = Set.copyOf(ranks);
        capabilities = Set.copyOf(capabilities);
    }

    public static WarpAccess publicWarp() {
        return new WarpAccess(true, Set.of(), Set.of());
    }
}
