package com.magicstudios.magiccore.storage;

import java.util.List;
import java.util.Optional;

public final class RecordRepository<T> {
    private final String namespace;
    private final JsonRecordCodec<T> codec;

    public RecordRepository(String namespace, Class<T> type) {
        this.namespace = namespace;
        this.codec = new JsonRecordCodec<>(type);
    }

    public Optional<VersionedValue<T>> get(DataReader reader, String key) throws Exception {
        return reader.get(namespace, key).map(record -> {
            try {
                return new VersionedValue<>(codec.decode(record.payload()), record.revision());
            } catch (Exception failure) {
                throw new IllegalStateException("Corrupt " + namespace + " record " + key, failure);
            }
        });
    }

    public VersionedValue<T> put(DataTransaction transaction, String key, T value, long expectedRevision) throws Exception {
        StoredRecord stored = transaction.put(namespace, key, codec.encode(value), expectedRevision);
        return new VersionedValue<>(value, stored.revision());
    }

    public boolean putIfAbsent(DataTransaction transaction, String key, T value) throws Exception {
        return transaction.putIfAbsent(namespace, key, codec.encode(value));
    }

    public List<VersionedValue<T>> scan(DataReader reader, String afterKey, int limit) throws Exception {
        return reader.scan(namespace, afterKey, limit).stream().map(record -> {
            try {
                return new VersionedValue<>(codec.decode(record.payload()), record.revision());
            } catch (Exception failure) {
                throw new IllegalStateException("Corrupt " + namespace + " record " + record.key(), failure);
            }
        }).toList();
    }

    public List<KeyedVersionedValue<T>> scanPage(DataReader reader, String afterKey, int limit) throws Exception {
        return reader.scan(namespace, afterKey, limit).stream().map(record -> {
            try {
                return new KeyedVersionedValue<>(record.key(), codec.decode(record.payload()), record.revision());
            } catch (Exception failure) {
                throw new IllegalStateException("Corrupt " + namespace + " record " + record.key(), failure);
            }
        }).toList();
    }

    public record VersionedValue<T>(T value, long revision) {
    }
    public record KeyedVersionedValue<T>(String key,T value,long revision){}
}
