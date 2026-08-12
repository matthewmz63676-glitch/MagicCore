package com.magicstudios.magiccore.storage;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class IdempotencyKeys {
    private static final String NAMESPACE = "_idempotency";

    private IdempotencyKeys() {
    }

    public static boolean reserve(DataTransaction transaction, String scope, String operationKey) throws Exception {
        Objects.requireNonNull(transaction, "transaction");
        String key = requireToken(scope, "scope") + ":" + requireToken(operationKey, "operationKey");
        return transaction.putIfAbsent(NAMESPACE, key, "reserved".getBytes(StandardCharsets.UTF_8));
    }

    private static String requireToken(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || value.length() > 191) {
            throw new IllegalArgumentException(field + " must be 1..191 characters");
        }
        return value;
    }
}
