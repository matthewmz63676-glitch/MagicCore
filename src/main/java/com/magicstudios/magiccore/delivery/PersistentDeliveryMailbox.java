package com.magicstudios.magiccore.delivery;

import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.JsonRecordCodec;
import com.magicstudios.magiccore.storage.StoredRecord;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentDeliveryMailbox implements DeliveryMailbox {
    private static final String NAMESPACE = DeliveryTransactionSupport.NAMESPACE;
    private final TransactionalDataStore store;
    private final JsonRecordCodec<MailboxDelivery> codec = new JsonRecordCodec<>(MailboxDelivery.class);
    private final Clock clock;

    public PersistentDeliveryMailbox(TransactionalDataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public CompletionStage<Boolean> enqueue(MailboxDelivery delivery) {
        return store.transact("mailbox-enqueue:" + delivery.operationKey(), transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "mailbox-enqueue", delivery.operationKey())) {
                return false;
            }
            DeliveryTransactionSupport.enqueue(transaction, delivery);
            return true;
        });
    }

    @Override
    public CompletionStage<List<MailboxDelivery>> pending(UUID recipientId, int limit) {
        String prefix = recipientId + ":";
        return store.read(reader -> reader.scan(NAMESPACE, null, Math.min(1000, Math.max(limit * 4, limit))).stream()
                .filter(record -> record.key().startsWith(prefix))
                .map(this::decode)
                .filter(delivery -> delivery.status() == DeliveryStatus.PENDING)
                .limit(limit)
                .toList());
    }

    @Override
    public CompletionStage<Boolean> markDelivered(UUID deliveryId, UUID recipientId, String operationKey) {
        return store.transact("mailbox-delivered:" + operationKey, transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "mailbox-delivered", operationKey)) {
                return false;
            }
            StoredRecord current = transaction.get(NAMESPACE, DeliveryTransactionSupport.key(recipientId, deliveryId)).orElse(null);
            if (current == null) {
                return false;
            }
            MailboxDelivery delivery = decode(current);
            if (delivery.status() == DeliveryStatus.DELIVERED) {
                return false;
            }
            MailboxDelivery delivered = new MailboxDelivery(delivery.id(), delivery.recipientId(),
                    delivery.operationKey(), delivery.payloadType(), delivery.payloadBase64(),
                    DeliveryStatus.DELIVERED, delivery.createdAt(), clock.instant());
            transaction.put(NAMESPACE, current.key(), codec.encode(delivered), current.revision());
            return true;
        });
    }

    private MailboxDelivery decode(StoredRecord record) {
        try {
            return codec.decode(record.payload());
        } catch (Exception failure) {
            throw new IllegalStateException("Corrupt mailbox delivery " + record.key(), failure);
        }
    }

}
