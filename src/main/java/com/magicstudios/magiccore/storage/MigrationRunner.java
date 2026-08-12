package com.magicstudios.magiccore.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public final class MigrationRunner {
    private static final String NAMESPACE = "_schema";
    private final TransactionalDataStore store;

    public MigrationRunner(TransactionalDataStore store) {
        this.store = store;
    }

    public CompletionStage<Map<String, Integer>> migrate(List<StorageMigration> migrations) {
        Map<String, List<StorageMigration>> byModule = new LinkedHashMap<>();
        migrations.stream().sorted(Comparator.comparing(StorageMigration::moduleId)
                        .thenComparingInt(StorageMigration::version))
                .forEach(migration -> byModule.computeIfAbsent(migration.moduleId(), ignored -> new ArrayList<>()).add(migration));
        return store.transact("schema-migrations", transaction -> {
            Map<String, Integer> applied = new LinkedHashMap<>();
            for (Map.Entry<String, List<StorageMigration>> entry : byModule.entrySet()) {
                String module = entry.getKey();
                StoredRecord versionRecord = transaction.get(NAMESPACE, module).orElse(null);
                int current = versionRecord == null ? 0 : ByteBuffer.wrap(versionRecord.payload()).getInt();
                long revision = versionRecord == null ? 0 : versionRecord.revision();
                for (StorageMigration migration : entry.getValue()) {
                    if (migration.version() <= current) {
                        continue;
                    }
                    if (migration.version() != current + 1) {
                        throw new IllegalStateException("Missing migration for " + module + " version " + (current + 1));
                    }
                    migration.work().execute(transaction);
                    current = migration.version();
                    StoredRecord updated = transaction.put(NAMESPACE, module,
                            ByteBuffer.allocate(Integer.BYTES).putInt(current).array(), revision);
                    revision = updated.revision();
                }
                applied.put(module, current);
            }
            return Map.copyOf(applied);
        });
    }
}
