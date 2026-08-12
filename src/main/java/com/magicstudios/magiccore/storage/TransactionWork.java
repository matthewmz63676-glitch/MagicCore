package com.magicstudios.magiccore.storage;

@FunctionalInterface
public interface TransactionWork<T> {
    T execute(DataTransaction transaction) throws Exception;
}
