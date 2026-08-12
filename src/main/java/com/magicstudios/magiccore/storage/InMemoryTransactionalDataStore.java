package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class InMemoryTransactionalDataStore implements TransactionalDataStore {
    private final BoundedIoExecutor executor;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, NavigableMap<String, Value>> state = new LinkedHashMap<>();

    public InMemoryTransactionalDataStore(BoundedIoExecutor executor) {
        this.executor = executor;
    }

    @Override
    public String providerId() {
        return "MEMORY";
    }

    @Override
    public StorageCapabilities capabilities() {
        return new StorageCapabilities(true, true, true, false);
    }

    @Override
    public CompletionStage<Void> start() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T> CompletionStage<T> read(ReadWork<T> work) {
        return executor.submit(() -> {
            lock.readLock().lock();
            try {
                return work.execute(new MemoryView(state));
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public <T> CompletionStage<T> transact(String operationName, TransactionWork<T> work) {
        return executor.submit(() -> {
            lock.writeLock().lock();
            try {
                Map<String, NavigableMap<String, Value>> candidate = copy(state);
                T result = work.execute(new MemoryTransaction(candidate));
                state = candidate;
                return result;
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public HealthReport health() {
        return HealthReport.healthy("storage:memory");
    }

    @Override
    public void close() {
        executor.close();
    }

    private static Map<String, NavigableMap<String, Value>> copy(Map<String, NavigableMap<String, Value>> source) {
        Map<String, NavigableMap<String, Value>> result = new LinkedHashMap<>();
        source.forEach((namespace, values) -> {
            NavigableMap<String, Value> copied = new TreeMap<>();
            values.forEach((key, value) -> copied.put(key, value.copy()));
            result.put(namespace, copied);
        });
        return result;
    }

    private record Value(byte[] payload, long revision) {
        private Value {
            payload = Arrays.copyOf(payload, payload.length);
        }

        private Value copy() {
            return new Value(payload, revision);
        }

        private StoredRecord stored(String key) {
            return new StoredRecord(key, payload, revision);
        }
    }

    private static class MemoryView implements DataReader {
        protected final Map<String, NavigableMap<String, Value>> state;

        private MemoryView(Map<String, NavigableMap<String, Value>> state) {
            this.state = state;
        }

        @Override
        public Optional<StoredRecord> get(String namespace, String key) {
            Value value = state.getOrDefault(namespace, new TreeMap<>()).get(key);
            return value == null ? Optional.empty() : Optional.of(value.stored(key));
        }

        @Override
        public List<StoredRecord> scan(String namespace, String afterKey, int limit) {
            if (limit < 1 || limit > 1000) {
                throw new IllegalArgumentException("limit must be between 1 and 1000");
            }
            NavigableMap<String, Value> values = state.getOrDefault(namespace, new TreeMap<>());
            NavigableMap<String, Value> page = afterKey == null ? values : values.tailMap(afterKey, false);
            List<StoredRecord> result = new ArrayList<>();
            page.entrySet().stream().limit(limit).forEach(entry -> result.add(entry.getValue().stored(entry.getKey())));
            return List.copyOf(result);
        }
    }

    private static final class MemoryTransaction extends MemoryView implements DataTransaction {
        private MemoryTransaction(Map<String, NavigableMap<String, Value>> state) {
            super(state);
        }

        @Override
        public StoredRecord put(String namespace, String key, byte[] payload, long expectedRevision) {
            NavigableMap<String, Value> values = state.computeIfAbsent(namespace, ignored -> new TreeMap<>());
            Value current = values.get(key);
            long actual = current == null ? 0 : current.revision();
            if (actual != expectedRevision) {
                throw new StorageConflictException(namespace, key, expectedRevision);
            }
            Value updated = new Value(payload, actual + 1);
            values.put(key, updated);
            return updated.stored(key);
        }

        @Override
        public boolean putIfAbsent(String namespace, String key, byte[] payload) {
            NavigableMap<String, Value> values = state.computeIfAbsent(namespace, ignored -> new TreeMap<>());
            if (values.containsKey(key)) {
                return false;
            }
            values.put(key, new Value(payload, 1));
            return true;
        }

        @Override
        public boolean delete(String namespace, String key, long expectedRevision) {
            NavigableMap<String, Value> values = state.get(namespace);
            if (values == null || !values.containsKey(key)) {
                return false;
            }
            if (values.get(key).revision() != expectedRevision) {
                throw new StorageConflictException(namespace, key, expectedRevision);
            }
            values.remove(key);
            return true;
        }
    }
}
