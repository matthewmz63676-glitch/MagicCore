package com.magicstudios.magiccore.storage;

@FunctionalInterface
public interface ReadWork<T> {
    T execute(DataReader reader) throws Exception;
}
