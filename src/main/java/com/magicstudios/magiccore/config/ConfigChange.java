package com.magicstudios.magiccore.config;

import java.util.Objects;
import java.util.function.UnaryOperator;

public record ConfigChange<T>(long expectedRevision, String actor, String source, String summary,
                              boolean restartRequired, UnaryOperator<T> mutation) {
    public ConfigChange {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision must not be negative");
        }
        actor = Objects.requireNonNull(actor, "actor");
        source = Objects.requireNonNull(source, "source");
        summary = Objects.requireNonNull(summary, "summary");
        mutation = Objects.requireNonNull(mutation, "mutation");
    }
}
