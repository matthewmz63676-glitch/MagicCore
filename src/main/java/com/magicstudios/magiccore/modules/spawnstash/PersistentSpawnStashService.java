package com.magicstudios.magiccore.modules.spawnstash;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.audit.AuditEvent;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public final class PersistentSpawnStashService implements SpawnStashService {
    private static final int MAX_SIGNALS = 512;
    private static final int MAX_NOTES = 128;
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final AuditService audit;
    private final Clock clock;
    private final boolean observeOnly;
    private final RecordRepository<SpawnStashCase> cases = new RecordRepository<>("spawnstash.case", SpawnStashCase.class);

    public PersistentSpawnStashService(TransactionalDataStore store, DomainEventBus events,
                                       AuditService audit, Clock clock, boolean observeOnly) {
        this.store = store; this.events = events; this.audit = audit; this.clock = clock; this.observeOnly = observeOnly;
    }

    @Override
    public CompletionStage<SpawnStashCase> prepare(UUID caseId, UUID targetId, UUID actorId, String actorName,
                                                   StashPosition origin, List<SpawnStashBlock> blocks,
                                                   Instant expiresAt, String operationKey) {
        if (blocks.isEmpty()) throw new IllegalArgumentException("SpawnStash needs at least one planned block");
        if (!expiresAt.isAfter(clock.instant())) throw new IllegalArgumentException("expiry must be in the future");
        SpawnStashCase prepared = new SpawnStashCase(caseId, targetId, actorId, actorName, origin,
                SpawnStashCase.Status.PREPARED, SpawnStashCase.Outcome.OPEN, observeOnly, blocks, List.of(), List.of(),
                clock.instant(), expiresAt, clock.instant(), null);
        return store.transact("spawnstash-prepare:" + operationKey, tx -> {
            Optional<RecordRepository.VersionedValue<SpawnStashCase>> existing = cases.get(tx, caseId.toString());
            if (existing.isPresent()) {
                if (existing.orElseThrow().value().equals(prepared)) return new Mutation(existing.orElseThrow().value(), false);
                throw new IllegalStateException("SpawnStash case ID already belongs to different parameters");
            }
            if (!IdempotencyKeys.reserve(tx, "spawnstash-operation", operationKey))
                throw new IllegalStateException("SpawnStash operation key was used for another case");
            if (!cases.putIfAbsent(tx, caseId.toString(), prepared)) throw new IllegalStateException("SpawnStash case already exists");
            return new Mutation(prepared, true);
        }).thenCompose(mutation -> audit(mutation, operationKey, "SPAWNSTASH_PREPARE", actorId.toString(), caseId.toString(),
                Map.of(), Map.of("target", targetId.toString(), "blocks", Integer.toString(blocks.size()), "observeOnly", Boolean.toString(observeOnly))))
                .thenApply(mutation -> { if (mutation.applied()) events.publish(new SpawnStashPrepared(caseId, targetId, actorId, blocks.size(), clock.instant())); return mutation.value(); });
    }

    @Override public CompletionStage<SpawnStashCase> markPlaced(UUID caseId, StashPosition position, String operationKey) {
        return mutate(caseId, operationKey, current -> {
            if(current.status()!=SpawnStashCase.Status.PREPARED)throw new IllegalStateException("Blocks can only be placed while a case is PREPARED");
            return replaceBlock(current, position, SpawnStashBlock.State.PLACED);
        }, "SPAWNSTASH_BLOCK_PLACED", "system", Map.of("position", key(position))).thenApply(Mutation::value);
    }

    @Override public CompletionStage<SpawnStashCase> activate(UUID caseId, String operationKey) {
        return mutate(caseId, operationKey, current -> {
            if (current.status() != SpawnStashCase.Status.PREPARED) return current;
            if (current.blocks().stream().noneMatch(block -> block.state() == SpawnStashBlock.State.PLACED))
                throw new IllegalStateException("Cannot activate a SpawnStash case without placed blocks");
            return copy(current, SpawnStashCase.Status.ACTIVE, current.outcome(), current.blocks(), current.signals(), current.notes(), null);
        }, "SPAWNSTASH_ACTIVATE", "system", Map.of()).thenApply(Mutation::value);
    }

    @Override
    public CompletionStage<SpawnStashCase> recordSignal(UUID caseId, SpawnStashSignal.Type type, UUID playerId,
                                                        StashPosition position, Map<String, String> details,
                                                        String operationKey) {
        SpawnStashSignal signal = new SpawnStashSignal(UUID.randomUUID(), type, playerId, position, details, clock.instant());
        return mutate(caseId, operationKey, current -> {
            if (current.status() != SpawnStashCase.Status.ACTIVE) return current;
            List<SpawnStashSignal> updated = new ArrayList<>(current.signals()); updated.add(signal);
            if (updated.size() > MAX_SIGNALS) updated = new ArrayList<>(updated.subList(updated.size() - MAX_SIGNALS, updated.size()));
            return copy(current, current.status(), current.outcome(), current.blocks(), updated, current.notes(), null);
        }, "SPAWNSTASH_SIGNAL_" + type, playerId.toString(), details).thenApply(mutation -> {
            if (mutation.applied()) events.publish(new SpawnStashSignalRecorded(caseId, mutation.value().targetId(), signal, clock.instant()));
            return mutation.value();
        });
    }

    @Override public CompletionStage<SpawnStashCase> addNote(UUID caseId, UUID actorId, String actorName, String note, String operationKey) {
        String text = checkedNote(note); SpawnStashNote entry = new SpawnStashNote(UUID.randomUUID(), actorId, actorName, text, clock.instant());
        return mutate(caseId, operationKey, current -> { if(current.status()==SpawnStashCase.Status.CLOSED)throw new IllegalStateException("Closed SpawnStash cases cannot be edited");List<SpawnStashNote> updated = new ArrayList<>(current.notes()); updated.add(entry);
            if (updated.size() > MAX_NOTES) updated = new ArrayList<>(updated.subList(updated.size() - MAX_NOTES, updated.size()));
            return copy(current, current.status(), current.outcome(), current.blocks(), current.signals(), updated, current.closedAt());
        }, "SPAWNSTASH_NOTE", actorId.toString(), Map.of("note", text)).thenApply(Mutation::value);
    }

    @Override
    public CompletionStage<SpawnStashCase> beginCleanup(UUID caseId, SpawnStashCase.Outcome outcome,
                                                        UUID actorId, String actorName, String note, String operationKey) {
        if (outcome == SpawnStashCase.Outcome.OPEN) throw new IllegalArgumentException("cleanup requires a final outcome");
        String text = checkedNote(note);
        return mutate(caseId, operationKey, current -> {
            if (current.status() == SpawnStashCase.Status.CLOSED) return current;
            List<SpawnStashNote> notes = new ArrayList<>(current.notes());
            notes.add(new SpawnStashNote(UUID.randomUUID(), actorId, actorName, text, clock.instant()));
            if(notes.size()>MAX_NOTES)notes=new ArrayList<>(notes.subList(notes.size()-MAX_NOTES,notes.size()));
            return copy(current, SpawnStashCase.Status.CLEANING, outcome, current.blocks(), current.signals(), notes, null);
        }, "SPAWNSTASH_BEGIN_CLEANUP", actorId.toString(), Map.of("outcome", outcome.name(), "note", text)).thenApply(Mutation::value);
    }

    @Override public CompletionStage<SpawnStashCase> markRestored(UUID caseId, StashPosition position, String operationKey) {
        return mutate(caseId, operationKey, current -> {if(current.status()!=SpawnStashCase.Status.CLEANING)throw new IllegalStateException("Blocks can only be restored while a case is CLEANING");return replaceBlock(current, position, SpawnStashBlock.State.RESTORED);},
                "SPAWNSTASH_BLOCK_RESTORED", "system", Map.of("position", key(position))).thenApply(Mutation::value);
    }

    @Override public CompletionStage<SpawnStashCase> completeCleanup(UUID caseId, String operationKey) {
        return mutate(caseId, operationKey, current -> {
            if (current.status() == SpawnStashCase.Status.CLOSED) return current;
            if (current.status() != SpawnStashCase.Status.CLEANING || current.blocks().stream().anyMatch(block -> block.state() != SpawnStashBlock.State.RESTORED))
                throw new IllegalStateException("SpawnStash cleanup is incomplete");
            return copy(current, SpawnStashCase.Status.CLOSED, current.outcome(), current.blocks(), current.signals(), current.notes(), clock.instant());
        }, "SPAWNSTASH_CLOSED", "system", Map.of()).thenApply(mutation -> {
            if (mutation.applied()) events.publish(new SpawnStashClosed(caseId, mutation.value().outcome(), clock.instant()));
            return mutation.value();
        });
    }

    @Override public CompletionStage<Optional<SpawnStashCase>> find(UUID caseId) {
        return store.read(reader -> cases.get(reader, caseId.toString()).map(RecordRepository.VersionedValue::value));
    }

    @Override public CompletionStage<List<SpawnStashCase>> openCases() { return scanOpen(null); }

    @Override public CompletionStage<List<SpawnStashCase>> activeForTarget(UUID targetId) {
        return scanOpen(targetId);
    }

    private CompletionStage<List<SpawnStashCase>> scanOpen(UUID targetId) {
        return store.read(reader -> { List<SpawnStashCase> result = new ArrayList<>(); String after = null;
            while (true) { var page = cases.scanPage(reader, after, 1000); for (var value : page) {
                SpawnStashCase candidate = value.value();
                if (candidate.status() != SpawnStashCase.Status.CLOSED && (targetId == null || candidate.targetId().equals(targetId))) result.add(candidate);
            } if (page.size() < 1000) break; after = page.get(page.size() - 1).key(); }
            return List.copyOf(result);
        });
    }

    private CompletionStage<Mutation> mutate(UUID caseId, String operationKey, UnaryOperator<SpawnStashCase> change,
                                             String action, String actor, Map<String, String> after) {
        return store.transact("spawnstash:" + operationKey, tx -> {
            var existing = cases.get(tx, caseId.toString()).orElseThrow(() -> new IllegalArgumentException("Unknown SpawnStash case"));
            if (!IdempotencyKeys.reserve(tx, "spawnstash-operation", operationKey)) return new Mutation(existing.value(), false);
            SpawnStashCase updated = change.apply(existing.value());
            if (updated == existing.value() || updated.equals(existing.value())) return new Mutation(existing.value(), false);
            cases.put(tx, caseId.toString(), updated, existing.revision());
            return new Mutation(updated, true);
        }).thenCompose(mutation -> audit(mutation, operationKey, action, actor, caseId.toString(), Map.of(), after));
    }

    private CompletionStage<Mutation> audit(Mutation mutation, String operationKey, String action, String actor,
                                            String target, Map<String, String> before, Map<String, String> after) {
        if (!mutation.applied()) return java.util.concurrent.CompletableFuture.completedFuture(mutation);
        AuditEvent event = new AuditEvent(UUID.randomUUID(), operationKey, action, actor, target, before, after,
                "MagicCore:SpawnStash", clock.instant());
        return audit.record(event).thenApply(ignored -> mutation);
    }

    private SpawnStashCase replaceBlock(SpawnStashCase current, StashPosition position, SpawnStashBlock.State state) {
        List<SpawnStashBlock> updated = new ArrayList<>(current.blocks()); boolean found = false;
        for (int i = 0; i < updated.size(); i++) { SpawnStashBlock block = updated.get(i); if (block.position().equals(position)) {
            if(block.state()==state)return current;
            updated.set(i, new SpawnStashBlock(block.position(), block.originalBlockData(), block.decoyBlockData(), block.lootAppearance(), state)); found = true; break;
        }}
        if (!found) throw new IllegalArgumentException("Position does not belong to SpawnStash case");
        return copy(current, current.status(), current.outcome(), updated, current.signals(), current.notes(), current.closedAt());
    }

    private SpawnStashCase copy(SpawnStashCase source, SpawnStashCase.Status status, SpawnStashCase.Outcome outcome,
                                List<SpawnStashBlock> blocks, List<SpawnStashSignal> signals,
                                List<SpawnStashNote> notes, Instant closedAt) {
        return new SpawnStashCase(source.id(), source.targetId(), source.actorId(), source.actorName(), source.origin(), status,
                outcome, source.observeOnly(), blocks, signals, notes, source.createdAt(), source.expiresAt(), clock.instant(), closedAt);
    }

    private static String checkedNote(String note) { if (note == null || note.isBlank() || note.length() > 500) throw new IllegalArgumentException("note must be 1-500 characters"); return note.trim(); }
    private static String key(StashPosition p) { return p.worldId() + ":" + p.x() + ":" + p.y() + ":" + p.z(); }
    private record Mutation(SpawnStashCase value, boolean applied) { }
}
