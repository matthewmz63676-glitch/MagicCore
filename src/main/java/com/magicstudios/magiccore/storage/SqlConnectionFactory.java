package com.magicstudios.magiccore.storage;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlConnectionFactory {
    Connection open() throws SQLException;
}
