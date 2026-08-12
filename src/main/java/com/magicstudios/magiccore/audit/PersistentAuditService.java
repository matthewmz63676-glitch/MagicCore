package com.magicstudios.magiccore.audit;

import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.JsonRecordCodec;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.util.List;
import java.util.concurrent.CompletionStage;

public final class PersistentAuditService implements AuditService {
    private static final String NAMESPACE = "audit.events";
    private final TransactionalDataStore store;
    private final JsonRecordCodec<AuditEvent> codec = new JsonRecordCodec<>(AuditEvent.class);

    public PersistentAuditService(TransactionalDataStore store) {
        this.store = store;
    }

    @Override
    public CompletionStage<Boolean> record(AuditEvent event) {
        return store.transact("audit:" + event.operationKey(), transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "audit", event.operationKey())) {
                return false;
            }
            String key = String.format("%013d:%s", event.timestamp().toEpochMilli(), event.id());
            transaction.put(NAMESPACE, key, codec.encode(event), 0);
            return true;
        });
    }

    @Override
    public CompletionStage<List<AuditEvent>> recent(String afterKey, int limit) {
        return store.read(reader -> reader.scan(NAMESPACE, afterKey, limit).stream()
                .map(record -> {
                    try {
                        return codec.decode(record.payload());
                    } catch (Exception failure) {
                        throw new IllegalStateException("Corrupt audit record " + record.key(), failure);
                    }
                }).toList());
    }
}
