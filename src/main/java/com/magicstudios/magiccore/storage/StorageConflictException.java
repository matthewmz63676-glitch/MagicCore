package com.magicstudios.magiccore.storage;

public final class StorageConflictException extends RuntimeException {
    public StorageConflictException(String namespace, String key, long expectedRevision) {
        super("Stale write for " + namespace + "/" + key + " at expected revision " + expectedRevision);
    }
}
