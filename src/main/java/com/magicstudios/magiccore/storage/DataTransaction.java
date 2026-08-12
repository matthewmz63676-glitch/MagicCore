package com.magicstudios.magiccore.storage;

public interface DataTransaction extends DataReader {
    StoredRecord put(String namespace, String key, byte[] payload, long expectedRevision) throws Exception;

    boolean putIfAbsent(String namespace, String key, byte[] payload) throws Exception;

    boolean delete(String namespace, String key, long expectedRevision) throws Exception;
}
