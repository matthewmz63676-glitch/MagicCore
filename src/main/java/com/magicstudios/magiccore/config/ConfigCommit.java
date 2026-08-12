package com.magicstudios.magiccore.config;

import java.nio.file.Path;
import java.util.List;

public record ConfigCommit<T>(ConfigSnapshot<T> snapshot, Path backup,
                              boolean restartRequired, List<ValidationIssue> warnings) {
    public ConfigCommit {
        warnings = List.copyOf(warnings);
    }
}
