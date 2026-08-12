package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import com.magicstudios.magiccore.storage.MigrationRunner;
import com.magicstudios.magiccore.storage.StorageMigration;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationRunnerTest {
    @Test
    void migrationsAreOrderedVersionedAndReplaySafe() {
        BoundedIoExecutor executor = new BoundedIoExecutor(1, 16, "migration-test");
        var store = new InMemoryTransactionalDataStore(executor);
        try {
            MigrationRunner runner = new MigrationRunner(store);
            List<StorageMigration> migrations = List.of(
                    new StorageMigration("profiles", 2, "second", tx -> {
                        tx.put("migration-data", "second", "2".getBytes(StandardCharsets.UTF_8), 0);
                        return null;
                    }),
                    new StorageMigration("profiles", 1, "first", tx -> {
                        tx.put("migration-data", "first", "1".getBytes(StandardCharsets.UTF_8), 0);
                        return null;
                    }));

            assertThat(runner.migrate(migrations).toCompletableFuture().join()).containsEntry("profiles", 2);
            assertThat(runner.migrate(migrations).toCompletableFuture().join()).containsEntry("profiles", 2);
            assertThat(store.read(reader -> reader.scan("migration-data", null, 10))
                    .toCompletableFuture().join()).hasSize(2);
        } finally {
            store.close();
        }
    }
}
