package com.magicstudios.magiccore.delivery;

import com.magicstudios.magiccore.storage.DataTransaction;
import com.magicstudios.magiccore.storage.JsonRecordCodec;

/** Allows a reward and its durable delivery to commit in the same storage transaction. */
public final class DeliveryTransactionSupport {
    public static final String NAMESPACE = "delivery.mailbox";
    private static final JsonRecordCodec<MailboxDelivery> CODEC = new JsonRecordCodec<>(MailboxDelivery.class);

    private DeliveryTransactionSupport() {
    }

    public static void enqueue(DataTransaction transaction, MailboxDelivery delivery) throws Exception {
        transaction.put(NAMESPACE, key(delivery.recipientId(), delivery.id()), CODEC.encode(delivery), 0);
    }

    public static String key(java.util.UUID recipientId, java.util.UUID deliveryId) {
        return recipientId + ":" + deliveryId;
    }
}
