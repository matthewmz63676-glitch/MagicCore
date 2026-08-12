package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class JdbcTransactionalDataStore implements TransactionalDataStore {
    private final String providerId;
    private final SqlConnectionFactory connections;
    private final BoundedIoExecutor executor;
    private final StorageCapabilities capabilities;
    private final AtomicReference<HealthReport> health;

    public JdbcTransactionalDataStore(String providerId, SqlConnectionFactory connections,
                                      BoundedIoExecutor executor, boolean networked) {
        this.providerId = providerId;
        this.connections = connections;
        this.executor = executor;
        this.capabilities = new StorageCapabilities(true, true, true, networked);
        this.health = new AtomicReference<>(new HealthReport("storage:" + providerId, HealthState.AVAILABLE,
                "not started", Map.of(), Instant.now()));
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public StorageCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public CompletionStage<Void> start() {
        return executor.submit(() -> {
            try (Connection connection = connections.open(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS magiccore_schema (module_id VARCHAR(96) PRIMARY KEY, version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS magiccore_records (namespace VARCHAR(96) NOT NULL, record_key VARCHAR(191) NOT NULL, payload BLOB NOT NULL, revision BIGINT NOT NULL, PRIMARY KEY(namespace, record_key))");
                health.set(HealthReport.healthy("storage:" + providerId));
                return null;
            } catch (Exception failure) {
                health.set(new HealthReport("storage:" + providerId, HealthState.FAILED,
                        failure.getClass().getSimpleName() + ": " + failure.getMessage(), Map.of(), Instant.now()));
                throw failure;
            }
        });
    }

    @Override
    public <T> CompletionStage<T> read(ReadWork<T> work) {
        return executor.submit(() -> {
            try (Connection connection = connections.open()) {
                return work.execute(new JdbcView(connection));
            }
        });
    }

    @Override
    public <T> CompletionStage<T> transact(String operationName, TransactionWork<T> work) {
        return executor.submit(() -> {
            try (Connection connection = connections.open()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.execute(new JdbcTransaction(connection));
                    connection.commit();
                    return result;
                } catch (Throwable failure) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackFailure) {
                        failure.addSuppressed(rollbackFailure);
                    }
                    throw failure;
                } finally {
                    connection.setAutoCommit(true);
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
        executor.close();
    }

    private static class JdbcView implements DataReader {
        protected final Connection connection;

        private JdbcView(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Optional<StoredRecord> get(String namespace, String key) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT payload, revision FROM magiccore_records WHERE namespace = ? AND record_key = ?")) {
                statement.setString(1, namespace);
                statement.setString(2, key);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(new StoredRecord(key, result.getBytes(1), result.getLong(2))) : Optional.empty();
                }
            }
        }

        @Override
        public List<StoredRecord> scan(String namespace, String afterKey, int limit) throws SQLException {
            if (limit < 1 || limit > 1000) {
                throw new IllegalArgumentException("limit must be between 1 and 1000");
            }
            String sql = afterKey == null
                    ? "SELECT record_key, payload, revision FROM magiccore_records WHERE namespace = ? ORDER BY record_key LIMIT ?"
                    : "SELECT record_key, payload, revision FROM magiccore_records WHERE namespace = ? AND record_key > ? ORDER BY record_key LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, namespace);
                if (afterKey == null) {
                    statement.setInt(2, limit);
                } else {
                    statement.setString(2, afterKey);
                    statement.setInt(3, limit);
                }
                List<StoredRecord> records = new ArrayList<>();
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        records.add(new StoredRecord(result.getString(1), result.getBytes(2), result.getLong(3)));
                    }
                }
                return List.copyOf(records);
            }
        }
    }

    private static final class JdbcTransaction extends JdbcView implements DataTransaction {
        private JdbcTransaction(Connection connection) {
            super(connection);
        }

        @Override
        public StoredRecord put(String namespace, String key, byte[] payload, long expectedRevision) throws SQLException {
            Optional<StoredRecord> current = get(namespace, key);
            long actual = current.map(StoredRecord::revision).orElse(0L);
            if (actual != expectedRevision) {
                throw new StorageConflictException(namespace, key, expectedRevision);
            }
            long updatedRevision = actual + 1;
            if (current.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO magiccore_records(namespace, record_key, payload, revision) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, namespace);
                    statement.setString(2, key);
                    statement.setBytes(3, payload);
                    statement.setLong(4, updatedRevision);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE magiccore_records SET payload = ?, revision = ? WHERE namespace = ? AND record_key = ? AND revision = ?")) {
                    statement.setBytes(1, payload);
                    statement.setLong(2, updatedRevision);
                    statement.setString(3, namespace);
                    statement.setString(4, key);
                    statement.setLong(5, expectedRevision);
                    if (statement.executeUpdate() != 1) {
                        throw new StorageConflictException(namespace, key, expectedRevision);
                    }
                }
            }
            return new StoredRecord(key, payload, updatedRevision);
        }

        @Override
        public boolean putIfAbsent(String namespace, String key, byte[] payload) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO magiccore_records(namespace, record_key, payload, revision) VALUES (?, ?, ?, 1)")) {
                statement.setString(1, namespace);
                statement.setString(2, key);
                statement.setBytes(3, payload);
                statement.executeUpdate();
                return true;
            } catch (SQLException failure) {
                if (get(namespace, key).isPresent()) {
                    return false;
                }
                throw failure;
            }
        }

        @Override
        public boolean delete(String namespace, String key, long expectedRevision) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM magiccore_records WHERE namespace = ? AND record_key = ? AND revision = ?")) {
                statement.setString(1, namespace);
                statement.setString(2, key);
                statement.setLong(3, expectedRevision);
                int changed = statement.executeUpdate();
                if (changed == 0 && get(namespace, key).isPresent()) {
                    throw new StorageConflictException(namespace, key, expectedRevision);
                }
                return changed == 1;
            }
        }
    }
}
