package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SqliteDataStoreFactory {
    private SqliteDataStoreFactory() {
    }

    public static TransactionalDataStore create(Path databaseFile, BoundedIoExecutor executor) {
        String url = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        SqlConnectionFactory connections = () -> {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException missingDriver) {
                throw new SQLException("SQLite JDBC driver is unavailable", missingDriver);
            }
            Connection connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
            }
            return connection;
        };
        return new JdbcTransactionalDataStore("SQLITE", connections, executor, false);
    }
}
