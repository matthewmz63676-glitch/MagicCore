package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.Money;
import com.magicstudios.magiccore.modules.economy.PersistentEconomyService;
import com.magicstudios.magiccore.modules.profiles.PersistentPlayerProfileService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileEconomyTest {
    @Test
    void uuidProfileOwnsNameHistoryLocaleAndSettings() {
        var store = store();
        try {
            UUID player = UUID.randomUUID();
            var profiles = new PersistentPlayerProfileService(store, new DomainEventBus());
            profiles.recordSeen(player, "FirstName", "en-us", Instant.parse("2026-08-01T00:00:00Z"))
                    .toCompletableFuture().join();
            profiles.recordSeen(player, "NewName", "fr_FR", Instant.parse("2026-08-02T00:00:00Z"))
                    .toCompletableFuture().join();
            profiles.setSetting(player, "profile.public", "false", "setting-1").toCompletableFuture().join();

            var profile = profiles.find(player).toCompletableFuture().join().orElseThrow();
            assertThat(profile.playerId()).isEqualTo(player);
            assertThat(profile.knownNames()).containsExactly("FirstName", "NewName");
            assertThat(profile.locale()).isEqualTo("fr_FR");
            assertThat(profile.settings()).containsEntry("profile.public", "false");
        } finally {
            store.close();
        }
    }

    @Test
    void exactLedgerMutationsAreIdempotentAndConcurrentTransfersConserveValue() {
        var store = store();
        try {
            CurrencyDefinition coins = new CurrencyDefinition("COINS", "Coins", "$", 2, 0, 1_000_000_000);
            var economy = new PersistentEconomyService(store, new DomainEventBus(), "COINS", Map.of("COINS", coins), Clock.systemUTC());
            UUID sender = UUID.randomUUID();
            UUID receiver = UUID.randomUUID();

            assertThat(economy.adjust(sender, new Money("COINS", 10_000), "admin", "seed", "seed-1")
                    .toCompletableFuture().join().applied()).isTrue();
            assertThat(economy.adjust(sender, new Money("COINS", 10_000), "admin", "seed", "seed-1")
                    .toCompletableFuture().join().applied()).isFalse();

            List<CompletableFuture<?>> transfers = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                transfers.add(economy.transfer(sender, receiver, new Money("COINS", 100), "pay-" + i)
                        .toCompletableFuture());
            }
            CompletableFuture.allOf(transfers.toArray(CompletableFuture[]::new)).join();

            assertThat(economy.balance(sender, "COINS").toCompletableFuture().join().minorUnits()).isEqualTo(8_000);
            assertThat(economy.balance(receiver, "COINS").toCompletableFuture().join().minorUnits()).isEqualTo(2_000);
            assertThat(economy.transactions(null, 100).toCompletableFuture().join()).hasSize(21);
        } finally {
            store.close();
        }
    }

    private static InMemoryTransactionalDataStore store() {
        return new InMemoryTransactionalDataStore(new BoundedIoExecutor(4, 256, "phase-one-test"));
    }
}
