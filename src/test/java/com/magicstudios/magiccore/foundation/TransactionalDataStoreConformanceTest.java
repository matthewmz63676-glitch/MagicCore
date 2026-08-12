package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import com.magicstudios.magiccore.storage.SqliteDataStoreFactory;
import com.magicstudios.magiccore.storage.StorageConflictException;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalDataStoreConformanceTest {
    @TempDir
    Path directory;

    @TestFactory
    Stream<DynamicNode> allProvidersUseIdenticalContract() {
        return Stream.of("MEMORY", "SQLITE").map(provider -> DynamicContainer.dynamicContainer(provider, tests(provider)));
    }

    private Stream<DynamicNode> tests(String provider) {
        return Stream.of(
                DynamicTest.dynamicTest("compare-and-swap", () -> withStore(provider, store -> {
                    var first = store.transact("insert", tx -> tx.put("test", "one", bytes("a"), 0))
                            .toCompletableFuture().join();
                    assertThat(first.revision()).isEqualTo(1);
                    assertThatThrownBy(() -> store.transact("stale", tx -> tx.put("test", "one", bytes("b"), 0))
                                    .toCompletableFuture().join())
                            .isInstanceOf(CompletionException.class).hasCauseInstanceOf(StorageConflictException.class);
                })),
                DynamicTest.dynamicTest("rollback", () -> withStore(provider, store -> {
                    assertThatThrownBy(() -> store.transact("rollback", tx -> {
                        tx.put("test", "rolled-back", bytes("value"), 0);
                        throw new IllegalStateException("fault injection");
                    }).toCompletableFuture().join()).isInstanceOf(CompletionException.class);
                    assertThat(store.read(reader -> reader.get("test", "rolled-back")).toCompletableFuture().join()).isEmpty();
                })),
                DynamicTest.dynamicTest("idempotency", () -> withStore(provider, store -> {
                    boolean first = store.transact("claim", tx -> IdempotencyKeys.reserve(tx, "reward", "daily:player:1"))
                            .toCompletableFuture().join();
                    boolean replay = store.transact("claim-replay", tx -> IdempotencyKeys.reserve(tx, "reward", "daily:player:1"))
                            .toCompletableFuture().join();
                    assertThat(first).isTrue();
                    assertThat(replay).isFalse();
                })),
                DynamicTest.dynamicTest("pagination", () -> withStore(provider, store -> {
                    store.transact("seed", tx -> {
                        tx.put("test", "a", bytes("1"), 0);
                        tx.put("test", "b", bytes("2"), 0);
                        tx.put("test", "c", bytes("3"), 0);
                        return null;
                    }).toCompletableFuture().join();
                    List<String> keys = store.read(reader -> reader.scan("test", "a", 2).stream()
                            .map(record -> record.key()).toList()).toCompletableFuture().join();
                    assertThat(keys).containsExactly("b", "c");
                }))
        );
    }

    private void withStore(String provider, ThrowingConsumer<TransactionalDataStore> test) throws Exception {
        BoundedIoExecutor executor = new BoundedIoExecutor(2, 64, "store-test");
        TransactionalDataStore store = provider.equals("MEMORY")
                ? new InMemoryTransactionalDataStore(executor)
                : SqliteDataStoreFactory.create(directory.resolve("store-" + System.nanoTime() + ".db"), executor);
        try {
            store.start().toCompletableFuture().join();
            store.capabilities().requireCriticalTransactions(provider);
            test.accept(store);
        } finally {
            store.close();
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
