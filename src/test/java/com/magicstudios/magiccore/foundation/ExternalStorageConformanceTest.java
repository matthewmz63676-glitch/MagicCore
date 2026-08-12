package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.MariaDbDataStoreFactory;
import com.magicstudios.magiccore.storage.MongoTransactionalDataStore;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalStorageConformanceTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "MAGICCORE_TEST_MARIADB_HOST", matches = ".+")
    void mariaDbPassesCriticalTransactionContract() {
        var store = MariaDbDataStoreFactory.create(System.getenv("MAGICCORE_TEST_MARIADB_HOST"),
                Integer.parseInt(System.getenv().getOrDefault("MAGICCORE_TEST_MARIADB_PORT", "3306")),
                System.getenv("MAGICCORE_TEST_MARIADB_DATABASE"), System.getenv("MAGICCORE_TEST_MARIADB_USERNAME"),
                System.getenv("MAGICCORE_TEST_MARIADB_PASSWORD"), new BoundedIoExecutor(2, 64, "mariadb-conformance"));
        assertContract(store);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MAGICCORE_TEST_MONGODB_URI", matches = ".+")
    void mongoDbPassesCriticalTransactionContract() {
        var store = new MongoTransactionalDataStore(System.getenv("MAGICCORE_TEST_MONGODB_URI"),
                System.getenv().getOrDefault("MAGICCORE_TEST_MONGODB_DATABASE", "magiccore_test"), true,
                new BoundedIoExecutor(2, 64, "mongodb-conformance"));
        assertContract(store);
    }

    private static void assertContract(TransactionalDataStore store) {
        String namespace = "conformance." + UUID.randomUUID();
        try {
            store.start().toCompletableFuture().join();
            store.capabilities().requireCriticalTransactions(store.providerId());
            boolean first = store.transact("conformance-insert", tx -> {
                tx.put(namespace, "balance", "100".getBytes(StandardCharsets.UTF_8), 0);
                return IdempotencyKeys.reserve(tx, namespace, "operation-1");
            }).toCompletableFuture().join();
            boolean replay = store.transact("conformance-replay", tx ->
                    IdempotencyKeys.reserve(tx, namespace, "operation-1")).toCompletableFuture().join();
            assertThat(first).isTrue();
            assertThat(replay).isFalse();
            assertThat(store.read(reader -> reader.get(namespace, "balance"))
                    .toCompletableFuture().join()).isPresent();
        } finally {
            store.close();
        }
    }
}
