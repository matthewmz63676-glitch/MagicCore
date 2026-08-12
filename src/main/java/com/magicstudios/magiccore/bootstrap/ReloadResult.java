package com.magicstudios.magiccore.bootstrap;

import java.util.List;

public record ReloadResult(boolean applied, boolean restartRequired, List<String> changedSections) {
    public ReloadResult {
        changedSections = List.copyOf(changedSections);
    }
}
