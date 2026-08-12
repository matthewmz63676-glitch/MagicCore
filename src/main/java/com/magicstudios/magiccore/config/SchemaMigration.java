package com.magicstudios.magiccore.config;

public interface SchemaMigration<T> {
    int fromVersion();

    int toVersion();

    T migrate(T source);
}
