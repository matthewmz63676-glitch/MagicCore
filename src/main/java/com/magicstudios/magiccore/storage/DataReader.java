package com.magicstudios.magiccore.storage;

import java.util.List;
import java.util.Optional;

public interface DataReader {
    Optional<StoredRecord> get(String namespace, String key) throws Exception;

    List<StoredRecord> scan(String namespace, String afterKey, int limit) throws Exception;
}
