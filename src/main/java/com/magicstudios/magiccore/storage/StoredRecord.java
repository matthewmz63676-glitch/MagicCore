package com.magicstudios.magiccore.storage;

import java.util.Arrays;
import java.util.Objects;

public record StoredRecord(String key, byte[] payload, long revision) {
    public StoredRecord {
        key = Objects.requireNonNull(key, "key");
        payload = Arrays.copyOf(payload, payload.length);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
    }

    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }
}
