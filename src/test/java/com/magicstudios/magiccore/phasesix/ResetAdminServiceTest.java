package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.audit.PersistentAuditService;
import com.magicstudios.magiccore.modules.resets.PersistentResetAdminService;
import com.magicstudios.magiccore.modules.resets.ResetJob;
import com.magicstudios.magiccore.modules.statistics.PersistentPlayerStatsService;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResetAdminServiceTest {
    @Test void serverResetBacksUpBatchesExcludesPlayersAndIsResumable() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "reset-test"));
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC);
            var events = new DomainEventBus(); var stats = new PersistentPlayerStatsService(store, events, clock);
            UUID first = UUID.randomUUID(), second = UUID.randomUUID(), excluded = UUID.randomUUID();
            stats.addPlaytime(first, 100, "first").toCompletableFuture().join();
            stats.addPlaytime(second, 200, "second").toCompletableFuture().join();
            stats.addPlaytime(excluded, 300, "excluded").toCompletableFuture().join();
            var service = new PersistentResetAdminService(store, new PersistentAuditService(store), events, clock, Duration.ofMinutes(5));
            ResetJob preview = service.previewServer(UUID.randomUUID(), Set.of("ALL"), Set.of(excluded), 1).toCompletableFuture().join();
            assertThat(preview.estimatedRecords()).isEqualTo(2);
            ResetJob complete = service.confirm(preview.id(), preview.confirmationToken(), "confirm").toCompletableFuture().join();
            assertThat(complete.status()).isEqualTo(ResetJob.Status.COMPLETE);
            assertThat(complete.processedRecords()).isEqualTo(2);
            assertThat(stats.stats(first).toCompletableFuture().join().playtimeSeconds()).isZero();
            assertThat(stats.stats(second).toCompletableFuture().join().playtimeSeconds()).isZero();
            assertThat(stats.stats(excluded).toCompletableFuture().join().playtimeSeconds()).isEqualTo(300);
            assertThat(store.read(reader -> reader.scan("resets.statistics-backup", null, 10).size()).toCompletableFuture().join()).isEqualTo(2);
            assertThat(service.resume(preview.id(), "resume-complete").toCompletableFuture().join().processedRecords()).isEqualTo(2);
        } finally { store.close(); }
    }

    @Test void playerResetRequiresTypedUnexpiredConfirmation() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(1, 32, "reset-confirmation"));
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC);
            var events = new DomainEventBus(); UUID player = UUID.randomUUID();
            var stats = new PersistentPlayerStatsService(store, events, clock); stats.addPlaytime(player, 10, "seed").toCompletableFuture().join();
            var service = new PersistentResetAdminService(store, new PersistentAuditService(store), events, clock, Duration.ofMinutes(5));
            ResetJob preview = service.previewPlayer(UUID.randomUUID(), player, Set.of("STATISTICS")).toCompletableFuture().join();
            assertThatThrownBy(() -> service.confirm(preview.id(), "RESET-WRONG", "wrong").toCompletableFuture().join())
                    .hasRootCauseInstanceOf(SecurityException.class);
            assertThat(stats.stats(player).toCompletableFuture().join().playtimeSeconds()).isEqualTo(10);
        } finally { store.close(); }
    }
}
