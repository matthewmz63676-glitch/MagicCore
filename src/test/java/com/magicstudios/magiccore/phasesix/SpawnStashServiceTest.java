package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.audit.PersistentAuditService;
import com.magicstudios.magiccore.modules.spawnstash.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpawnStashServiceTest {
    @Test
    void caseIsObserveOnlyReplaySafeReviewedAndCrashResumable() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "spawnstash-test"));
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC);
            var audit = new PersistentAuditService(store); var events = new DomainEventBus();
            var service = new PersistentSpawnStashService(store, events, audit, clock, true);
            UUID caseId = UUID.randomUUID(), target = UUID.randomUUID(), actor = UUID.randomUUID(), world = UUID.randomUUID();
            StashPosition first = new StashPosition(world, 10, 60, 10), second = new StashPosition(world, 11, 60, 10);
            List<SpawnStashBlock> blocks = List.of(block(first), block(second));

            SpawnStashCase prepared = service.prepare(caseId, target, actor, "Staff", first, blocks,
                    clock.instant().plusSeconds(1800), "prepare-1").toCompletableFuture().join();
            assertThat(prepared.observeOnly()).isTrue();
            assertThat(service.prepare(caseId, target, actor, "Staff", first, blocks,
                    clock.instant().plusSeconds(1800), "prepare-1").toCompletableFuture().join()).isEqualTo(prepared);
            service.markPlaced(caseId, first, "placed-1").toCompletableFuture().join();
            service.markPlaced(caseId, second, "placed-2").toCompletableFuture().join();
            service.activate(caseId, "activate-1").toCompletableFuture().join();
            SpawnStashCase signaled = service.recordSignal(caseId, SpawnStashSignal.Type.VULCAN_FLAG, target, first,
                    Map.of("check", "XRay", "vl", "3.0"), "signal-1").toCompletableFuture().join();
            assertThat(signaled.signals()).singleElement().extracting(SpawnStashSignal::type)
                    .isEqualTo(SpawnStashSignal.Type.VULCAN_FLAG);
            service.addNote(caseId, actor, "Staff", "Review started", "note-1").toCompletableFuture().join();
            SpawnStashCase cleaning = service.beginCleanup(caseId, SpawnStashCase.Outcome.FALSE_POSITIVE,
                    actor, "Staff", "Natural pathing; close as false positive", "cleanup-1").toCompletableFuture().join();
            assertThat(cleaning.status()).isEqualTo(SpawnStashCase.Status.CLEANING);
            service.markRestored(caseId, first, "restore-1").toCompletableFuture().join();
            assertThatThrownBy(() -> service.completeCleanup(caseId, "close-too-early").toCompletableFuture().join())
                    .hasRootCauseMessage("SpawnStash cleanup is incomplete");

            // Simulate restart: a new service discovers the cleaning checkpoint and resumes remaining blocks.
            var restarted = new PersistentSpawnStashService(store, new DomainEventBus(), audit, clock, true);
            assertThat(restarted.openCases().toCompletableFuture().join()).singleElement()
                    .extracting(SpawnStashCase::status).isEqualTo(SpawnStashCase.Status.CLEANING);
            restarted.markRestored(caseId, second, "restore-2").toCompletableFuture().join();
            SpawnStashCase closed = restarted.completeCleanup(caseId, "close-1").toCompletableFuture().join();
            assertThat(closed.status()).isEqualTo(SpawnStashCase.Status.CLOSED);
            assertThat(closed.outcome()).isEqualTo(SpawnStashCase.Outcome.FALSE_POSITIVE);
            assertThat(closed.blocks()).allMatch(block -> block.state() == SpawnStashBlock.State.RESTORED);
            assertThat(restarted.openCases().toCompletableFuture().join()).isEmpty();
            assertThat(audit.recent(null, 100).toCompletableFuture().join()).hasSizeGreaterThanOrEqualTo(10);
        } finally { store.close(); }
    }

    private static SpawnStashBlock block(StashPosition position) {
        return new SpawnStashBlock(position, "minecraft:stone", "minecraft:chest",
                List.of(new SpawnStashBlock.LootAppearance("DIAMOND", 1)), SpawnStashBlock.State.PLANNED);
    }
}
