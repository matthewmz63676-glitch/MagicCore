package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.crates.*;
import com.magicstudios.magiccore.modules.keyall.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class KeyallServiceTest {
    @Test void previewsCancelsAndDeliversEveryRecipientExactlyOnce() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "keyall-test"));
        try {
            var crates = new RecordingCrates(); var service = service(store, crates, 10);
            UUID first = UUID.randomUUID(), second = UUID.randomUUID();
            KeyallRun cancelled = service.preview("VOTE_KEYALL", KeyallRun.Trigger.MANUAL, List.of(first, second)).thenCompose(run -> service.cancel(run.id(), "cancel")).toCompletableFuture().join();
            assertThat(cancelled.status()).isEqualTo(KeyallRun.Status.CANCELLED); assertThat(crates.calls).hasValue(0);
            KeyallRun preview = service.preview("VOTE_KEYALL", KeyallRun.Trigger.MANUAL, List.of(second, first, first)).toCompletableFuture().join();
            assertThat(preview.recipients()).hasSize(2);
            KeyallRun complete = service.execute(preview.id(), "execute").toCompletableFuture().join();
            assertThat(complete.status()).isEqualTo(KeyallRun.Status.COMPLETE); assertThat(complete.delivered()).isEqualTo(2);
            assertThat(crates.calls).hasValue(2);
            assertThat(service.execute(preview.id(), "replay").toCompletableFuture().join().delivered()).isEqualTo(2);
            assertThat(crates.calls).hasValue(2);
        } finally { store.close(); }
    }

    @Test void thresholdIsIdempotentAndProviderFailuresAreReportedWithoutBlockingOthers() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "keyall-threshold"));
        try {
            var crates = new RecordingCrates(); UUID failing = UUID.randomUUID(); crates.fail.add(failing);
            var service = service(store, crates, 3); List<UUID> audience = List.of(UUID.randomUUID(), failing);
            assertThat(service.contribute("VOTE_KEYALL", 4, audience, "votes-1").toCompletableFuture().join()).isEmpty();
            var triggered = service.contribute("VOTE_KEYALL", 6, audience, "votes-2").toCompletableFuture().join().orElseThrow();
            assertThat(triggered.status()).isEqualTo(KeyallRun.Status.PARTIAL); assertThat(triggered.delivered()).isEqualTo(1);
            assertThat(triggered.failures()).containsKey(failing);
            assertThat(service.contribute("VOTE_KEYALL", 6, audience, "votes-2").toCompletableFuture().join()).isEmpty();
        } finally { store.close(); }
    }

    private static PersistentKeyallService service(InMemoryTransactionalDataStore store, CrateService crates, int max) {
        return new PersistentKeyallService(store, crates, new DomainEventBus(), Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC),
                List.of(new KeyallDefinition("VOTE_KEYALL", "VOTE_KEY", 1, KeyallDefinition.Audience.ONLINE, false, Duration.ZERO, 10)), max);
    }

    private static final class RecordingCrates implements CrateService {
        private final AtomicInteger calls = new AtomicInteger(); private final Set<UUID> fail = ConcurrentHashMap.newKeySet(); private final Map<UUID,Long> balances = new ConcurrentHashMap<>();
        @Override public Map<String, CrateDefinition> definitions() { return Map.of(); }
        @Override public CompletionStage<CrateKeyBalance> keyBalance(UUID playerId, String keyId) { return CompletableFuture.completedFuture(new CrateKeyBalance(playerId, keyId, balances.getOrDefault(playerId, 0L), Instant.now())); }
        @Override public CompletionStage<CrateKeyBalance> grantKeys(UUID playerId, String keyId, long amount, String operationKey) { calls.incrementAndGet(); if (fail.contains(playerId)) return CompletableFuture.failedFuture(new IllegalStateException("provider unavailable")); long balance = balances.merge(playerId, amount, Long::sum); return CompletableFuture.completedFuture(new CrateKeyBalance(playerId, keyId, balance, Instant.now())); }
        @Override public CompletionStage<CrateOpenResult> open(UUID playerId, String crateId, int amount, String operationKey) { throw new UnsupportedOperationException(); }
        @Override public CompletionStage<List<CrateOpening>> history(UUID playerId, int limit) { return CompletableFuture.completedFuture(List.of()); }
    }
}
