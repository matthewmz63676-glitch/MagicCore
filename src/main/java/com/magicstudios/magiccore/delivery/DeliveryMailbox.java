package com.magicstudios.magiccore.delivery;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface DeliveryMailbox {
    CompletionStage<Boolean> enqueue(MailboxDelivery delivery);

    CompletionStage<List<MailboxDelivery>> pending(UUID recipientId, int limit);

    CompletionStage<Boolean> markDelivered(UUID deliveryId, UUID recipientId, String operationKey);
}
