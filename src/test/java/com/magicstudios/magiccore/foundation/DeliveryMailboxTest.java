package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryMailboxTest {
    @Test
    void valuableDeliveryIsDurableAndIdempotent() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(1, 16, "mailbox-test"));
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
            var mailbox = new PersistentDeliveryMailbox(store, clock);
            UUID player = UUID.randomUUID();
            UUID deliveryId = UUID.randomUUID();
            MailboxDelivery delivery = MailboxDelivery.pending(deliveryId, player, "reward:daily:1",
                    "minecraft-item", "valuable".getBytes(StandardCharsets.UTF_8), clock.instant());

            assertThat(mailbox.enqueue(delivery).toCompletableFuture().join()).isTrue();
            assertThat(mailbox.enqueue(delivery).toCompletableFuture().join()).isFalse();
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).singleElement()
                    .extracting(MailboxDelivery::operationKey).isEqualTo("reward:daily:1");
            assertThat(mailbox.markDelivered(deliveryId, player, "deliver:1").toCompletableFuture().join()).isTrue();
            assertThat(mailbox.markDelivered(deliveryId, player, "deliver:1").toCompletableFuture().join()).isFalse();
            assertThat(mailbox.pending(player, 10).toCompletableFuture().join()).isEmpty();
        } finally {
            store.close();
        }
    }
}
