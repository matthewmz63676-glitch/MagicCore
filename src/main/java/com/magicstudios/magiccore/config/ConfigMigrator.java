package com.magicstudios.magiccore.config;

import java.util.Comparator;
import java.util.List;

public final class ConfigMigrator<T> {
    private final List<SchemaMigration<T>> migrations;

    public ConfigMigrator(List<SchemaMigration<T>> migrations) {
        this.migrations = migrations.stream().sorted(Comparator.comparingInt(SchemaMigration::fromVersion)).toList();
    }

    public T migrate(int currentVersion, int targetVersion, T value) {
        int version = currentVersion;
        T candidate = value;
        while (version < targetVersion) {
            int expected = version;
            SchemaMigration<T> migration = migrations.stream()
                    .filter(item -> item.fromVersion() == expected).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No configuration migration from version " + expected));
            if (migration.toVersion() <= version) {
                throw new IllegalStateException("Migration must advance the version from " + version);
            }
            candidate = migration.migrate(candidate);
            version = migration.toVersion();
        }
        if (version != targetVersion) {
            throw new IllegalStateException("Migration ended at version " + version + " instead of " + targetVersion);
        }
        return candidate;
    }
}
