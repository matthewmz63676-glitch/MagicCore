package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.Binary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class MongoTransactionalDataStore implements TransactionalDataStore {
    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> records;
    private final BoundedIoExecutor executor;
    private final boolean requireTransactions;
    private final AtomicReference<StorageCapabilities> capabilities = new AtomicReference<>(
            new StorageCapabilities(false, false, true, true));
    private final AtomicReference<HealthReport> health = new AtomicReference<>(new HealthReport(
            "storage:MONGODB", HealthState.AVAILABLE, "not started", Map.of(), Instant.now()));

    public MongoTransactionalDataStore(String connectionString, String databaseName,
                                       boolean requireTransactions, BoundedIoExecutor executor) {
        this.client = MongoClients.create(connectionString);
        this.database = client.getDatabase(databaseName);
        this.records = database.getCollection("magiccore_records");
        this.requireTransactions = requireTransactions;
        this.executor = executor;
    }

    @Override
    public String providerId() {
        return "MONGODB";
    }

    @Override
    public StorageCapabilities capabilities() {
        return capabilities.get();
    }

    @Override
    public CompletionStage<Void> start() {
        return executor.submit(() -> {
            try {
                Document hello = database.runCommand(new Document("hello", 1));
                boolean transactions = MongoTransactionCapability.supportsTransactions(hello);
                StorageCapabilities detected = new StorageCapabilities(transactions, transactions, true, true);
                capabilities.set(detected);
                if (requireTransactions) {
                    detected.requireCriticalTransactions("MONGODB");
                    probeTransaction();
                }
                records.createIndex(Indexes.compoundIndex(Indexes.ascending("namespace"), Indexes.ascending("key")),
                        new IndexOptions().unique(true));
                health.set(HealthReport.healthy("storage:MONGODB"));
                return null;
            } catch (Exception failure) {
                health.set(new HealthReport("storage:MONGODB", HealthState.FAILED,
                        failure.getClass().getSimpleName() + ": " + failure.getMessage(), Map.of(), Instant.now()));
                throw failure;
            }
        });
    }

    @Override
    public <T> CompletionStage<T> read(ReadWork<T> work) {
        return executor.submit(() -> work.execute(new MongoView(records, null)));
    }

    @Override
    public <T> CompletionStage<T> transact(String operationName, TransactionWork<T> work) {
        return executor.submit(() -> {
            capabilities().requireCriticalTransactions("MONGODB");
            try (ClientSession session = client.startSession(ClientSessionOptions.builder().causallyConsistent(true).build())) {
                session.startTransaction(transactionOptions());
                try {
                    T result = work.execute(new MongoTransaction(records, session));
                    session.commitTransaction();
                    return result;
                } catch (Throwable failure) {
                    try {
                        session.abortTransaction();
                    } catch (RuntimeException abortFailure) {
                        failure.addSuppressed(abortFailure);
                    }
                    throw failure;
                }
            }
        });
    }

    @Override
    public HealthReport health() {
        return health.get();
    }

    @Override
    public void close() {
        client.close();
        executor.close();
    }

    private void probeTransaction() {
        try (ClientSession session = client.startSession()) {
            session.startTransaction(transactionOptions());
            database.runCommand(session, new Document("ping", 1));
            session.abortTransaction();
        }
    }

    private static TransactionOptions transactionOptions() {
        return TransactionOptions.builder()
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .build();
    }

    private static class MongoView implements DataReader {
        protected final MongoCollection<Document> records;
        protected final ClientSession session;

        private MongoView(MongoCollection<Document> records, ClientSession session) {
            this.records = records;
            this.session = session;
        }

        @Override
        public Optional<StoredRecord> get(String namespace, String key) {
            Document result = iterable(Filters.and(Filters.eq("namespace", namespace), Filters.eq("key", key))).first();
            return Optional.ofNullable(result).map(MongoView::toRecord);
        }

        @Override
        public List<StoredRecord> scan(String namespace, String afterKey, int limit) {
            if (limit < 1 || limit > 1000) {
                throw new IllegalArgumentException("limit must be between 1 and 1000");
            }
            org.bson.conversions.Bson filter = afterKey == null
                    ? Filters.eq("namespace", namespace)
                    : Filters.and(Filters.eq("namespace", namespace), Filters.gt("key", afterKey));
            List<StoredRecord> result = new ArrayList<>();
            iterable(filter).sort(new Document("key", 1)).limit(limit).map(MongoView::toRecord).into(result);
            return List.copyOf(result);
        }

        protected FindIterable<Document> iterable(org.bson.conversions.Bson filter) {
            return session == null ? records.find(filter) : records.find(session, filter);
        }

        protected static StoredRecord toRecord(Document document) {
            Object payload = document.get("payload");
            byte[] bytes = payload instanceof Binary binary ? binary.getData() : (byte[]) payload;
            return new StoredRecord(document.getString("key"), bytes, document.getLong("revision"));
        }
    }

    private static final class MongoTransaction extends MongoView implements DataTransaction {
        private MongoTransaction(MongoCollection<Document> records, ClientSession session) {
            super(records, session);
        }

        @Override
        public StoredRecord put(String namespace, String key, byte[] payload, long expectedRevision) {
            Optional<StoredRecord> current = get(namespace, key);
            long actual = current.map(StoredRecord::revision).orElse(0L);
            if (actual != expectedRevision) {
                throw new StorageConflictException(namespace, key, expectedRevision);
            }
            long revision = actual + 1;
            Document replacement = new Document("namespace", namespace).append("key", key)
                    .append("payload", new Binary(payload)).append("revision", revision);
            if (actual == 0) {
                try {
                    records.insertOne(session, replacement);
                } catch (MongoWriteException duplicate) {
                    throw new StorageConflictException(namespace, key, expectedRevision);
                }
            } else {
                long changed = records.replaceOne(session,
                        Filters.and(Filters.eq("namespace", namespace), Filters.eq("key", key), Filters.eq("revision", expectedRevision)),
                        replacement).getModifiedCount();
                if (changed != 1) {
                    throw new StorageConflictException(namespace, key, expectedRevision);
                }
            }
            return new StoredRecord(key, payload, revision);
        }

        @Override
        public boolean putIfAbsent(String namespace, String key, byte[] payload) {
            try {
                records.insertOne(session, new Document("namespace", namespace).append("key", key)
                        .append("payload", new Binary(payload)).append("revision", 1L));
                return true;
            } catch (MongoWriteException duplicate) {
                if (duplicate.getError().getCode() == 11000) {
                    return false;
                }
                throw duplicate;
            }
        }

        @Override
        public boolean delete(String namespace, String key, long expectedRevision) {
            long changed = records.deleteOne(session, Filters.and(Filters.eq("namespace", namespace),
                    Filters.eq("key", key), Filters.eq("revision", expectedRevision))).getDeletedCount();
            if (changed == 0 && get(namespace, key).isPresent()) {
                throw new StorageConflictException(namespace, key, expectedRevision);
            }
            return changed == 1;
        }
    }
}
