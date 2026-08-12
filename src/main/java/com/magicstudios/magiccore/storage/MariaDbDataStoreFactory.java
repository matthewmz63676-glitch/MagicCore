package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;

import java.sql.DriverManager;

public final class MariaDbDataStoreFactory {
    private MariaDbDataStoreFactory() {
    }

    public static TransactionalDataStore create(String host, int port, String database,
                                                String username, String password,
                                                BoundedIoExecutor executor) {
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8&useServerPrepStmts=true";
        return new JdbcTransactionalDataStore("MARIADB",
                () -> DriverManager.getConnection(url, username, password), executor, true);
    }
}
