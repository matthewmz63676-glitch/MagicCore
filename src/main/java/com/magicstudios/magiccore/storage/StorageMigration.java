package com.magicstudios.magiccore.storage;

import java.util.Objects;

public record StorageMigration(String moduleId, int version, String description, TransactionWork<Void> work) {
    public StorageMigration {
        moduleId = Objects.requireNonNull(moduleId, "moduleId");
        if (version < 1) {
            throw new IllegalArgumentException("Migration version must be positive");
        }
        description = Objects.requireNonNull(description, "description");
        work = Objects.requireNonNull(work, "work");
    }
}
