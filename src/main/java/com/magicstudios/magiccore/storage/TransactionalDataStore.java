package com.magicstudios.magiccore.storage;

import com.magicstudios.magiccore.api.HealthReport;

import java.util.concurrent.CompletionStage;

public interface TransactionalDataStore extends AutoCloseable {
    String providerId();

    StorageCapabilities capabilities();

    CompletionStage<Void> start();

    <T> CompletionStage<T> read(ReadWork<T> work);

    <T> CompletionStage<T> transact(String operationName, TransactionWork<T> work);

    HealthReport health();

    @Override
    void close();
}
