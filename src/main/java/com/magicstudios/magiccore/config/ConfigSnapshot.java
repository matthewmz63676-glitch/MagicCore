package com.magicstudios.magiccore.config;

import java.time.Instant;
import java.util.Objects;

public record ConfigSnapshot<T>(long revision, T value, Instant loadedAt, String contentHash) {
    public ConfigSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        value = Objects.requireNonNull(value, "value");
        loadedAt = Objects.requireNonNull(loadedAt, "loadedAt");
        contentHash = Objects.requireNonNull(contentHash, "contentHash");
    }
}
