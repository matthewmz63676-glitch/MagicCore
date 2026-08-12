package com.magicstudios.magiccore.config.model;

public record StorageFile(int configVersion, String provider, Sqlite sqlite,
                          MariaDb mariadb, MongoDb mongodb, Timeouts timeouts) {
    public record Sqlite(String file) { }
    public record MariaDb(String host, int port, String database, String username,
                          String passwordFrom, int poolSize) { }
    public record MongoDb(String connectionStringFrom, String database, boolean requireTransactions) { }
    public record Timeouts(int connectSeconds, int operationSeconds) { }
}
