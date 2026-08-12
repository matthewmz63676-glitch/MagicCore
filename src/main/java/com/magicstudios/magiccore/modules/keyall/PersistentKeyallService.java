package com.magicstudios.magiccore.modules.keyall;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentKeyallService implements KeyallService {
    private final TransactionalDataStore store;
    private final CrateService crates;
    private final DomainEventBus events;
    private final Clock clock;
    private final Map<String,KeyallDefinition> definitions;
    private final int maximumRecipients;
    private final RecordRepository<KeyallRun> runs = new RecordRepository<>("keyall.run", KeyallRun.class);
    private final RecordRepository<KeyallDelivery> deliveries = new RecordRepository<>("keyall.delivery", KeyallDelivery.class);
    private final RecordRepository<KeyallThreshold> thresholds = new RecordRepository<>("keyall.threshold", KeyallThreshold.class);

    public PersistentKeyallService(TransactionalDataStore store, CrateService crates, DomainEventBus events, Clock clock,
                                   Collection<KeyallDefinition> definitions, int maximumRecipients) {
        this.store = store; this.crates = crates; this.events = events; this.clock = clock; this.maximumRecipients = maximumRecipients;
        LinkedHashMap<String,KeyallDefinition> indexed = new LinkedHashMap<>();
        for (KeyallDefinition definition : definitions) if (indexed.putIfAbsent(definition.id(), definition) != null)
            throw new IllegalArgumentException("Duplicate keyall definition " + definition.id());
        this.definitions = Map.copyOf(indexed);
    }

    @Override public Map<String, KeyallDefinition> definitions() { return definitions; }

    @Override public CompletionStage<KeyallRun> preview(String definitionId, KeyallRun.Trigger trigger, Collection<UUID> recipients) {
        require(definitionId); List<UUID> audience = recipients.stream().distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        if (audience.size() > maximumRecipients) throw new IllegalArgumentException("KEYALL_AUDIENCE_TOO_LARGE");
        KeyallRun run = new KeyallRun(UUID.randomUUID(), definitionId, trigger, KeyallRun.Status.PREVIEWED,
                audience, 0, Map.of(), clock.instant(), clock.instant());
        return store.transact("keyall-preview:" + run.id(), tx -> { runs.put(tx, run.id().toString(), run, 0); return run; });
    }

    @Override public CompletionStage<KeyallRun> execute(UUID runId, String operationKey) {
        return store.transact("keyall-execute:" + operationKey, tx -> {
            var current = runs.get(tx, runId.toString()).orElseThrow(() -> new IllegalArgumentException("KEYALL_RUN_NOT_FOUND"));
            KeyallRun run = current.value();
            if (run.status() == KeyallRun.Status.COMPLETE || run.status() == KeyallRun.Status.PARTIAL) return run;
            if (!IdempotencyKeys.reserve(tx, "keyall-execute", operationKey)) return run;
            if (run.status() != KeyallRun.Status.PREVIEWED && run.status() != KeyallRun.Status.RUNNING) throw new IllegalStateException("KEYALL_RUN_UNAVAILABLE");
            KeyallRun running = copy(run, KeyallRun.Status.RUNNING, run.delivered(), run.failures());
            runs.put(tx, runId.toString(), running, current.revision()); return running;
        }).thenCompose(run -> run.status() == KeyallRun.Status.RUNNING ? deliver(run, 0) : CompletableFuture.completedFuture(run));
    }

    private CompletionStage<KeyallRun> deliver(KeyallRun run, int index) {
        if (index >= run.recipients().size()) return finish(run.id());
        UUID player = run.recipients().get(index); String deliveryKey = run.id() + ":" + player;
        return store.read(reader -> deliveries.get(reader, deliveryKey).map(RecordRepository.VersionedValue::value)).thenCompose(existing -> {
            if (existing.isPresent()) return deliver(run, index + 1);
            KeyallDefinition definition = require(run.definitionId());
            return crates.grantKeys(player, definition.keyId(), definition.amount(), "keyall:" + deliveryKey)
                    .handle((balance, failure) -> failure == null
                            ? new KeyallDelivery(run.id(), player, definition.keyId(), definition.amount(), KeyallDelivery.Status.DELIVERED, "DELIVERED", clock.instant())
                            : new KeyallDelivery(run.id(), player, definition.keyId(), definition.amount(), KeyallDelivery.Status.FAILED,
                            root(failure).getClass().getSimpleName() + ":" + String.valueOf(root(failure).getMessage()), clock.instant()))
                    .thenCompose(delivery -> checkpoint(deliveryKey, delivery)).thenCompose(ignored -> deliver(run, index + 1));
        });
    }

    private CompletionStage<KeyallDelivery> checkpoint(String key, KeyallDelivery delivery) {
        return store.transact("keyall-delivery:" + key, tx -> {
            var existing = deliveries.get(tx, key); if (existing.isPresent()) return existing.get().value();
            deliveries.put(tx, key, delivery, 0); return delivery;
        });
    }

    private CompletionStage<KeyallRun> finish(UUID runId) {
        return store.transact("keyall-finish:" + runId, tx -> {
            var current = runs.get(tx, runId.toString()).orElseThrow(); KeyallRun run = current.value();
            if (run.status() != KeyallRun.Status.RUNNING) return run;
            List<KeyallDelivery> results = scanDeliveries(tx, runId); Map<UUID,String> failures = new LinkedHashMap<>();
            results.stream().filter(value -> value.status() == KeyallDelivery.Status.FAILED).forEach(value -> failures.put(value.playerId(), value.detail()));
            int delivered = (int) results.stream().filter(value -> value.status() == KeyallDelivery.Status.DELIVERED).count();
            KeyallRun completed = copy(run, failures.isEmpty() ? KeyallRun.Status.COMPLETE : KeyallRun.Status.PARTIAL, delivered, failures);
            runs.put(tx, runId.toString(), completed, current.revision()); return completed;
        }).thenApply(run -> { events.publish(new KeyallCompleted(run.id(), run.definitionId(), run.delivered(), run.failures().size(), clock.instant())); return run; });
    }

    @Override public CompletionStage<KeyallRun> cancel(UUID runId, String operationKey) {
        return store.transact("keyall-cancel:" + operationKey, tx -> {
            var current = runs.get(tx, runId.toString()).orElseThrow(() -> new IllegalArgumentException("KEYALL_RUN_NOT_FOUND"));
            if (!IdempotencyKeys.reserve(tx, "keyall-cancel", operationKey)) return current.value();
            if (current.value().status() != KeyallRun.Status.PREVIEWED) throw new IllegalStateException("ONLY_PREVIEWED_KEYALLS_CAN_BE_CANCELLED");
            KeyallRun cancelled = copy(current.value(), KeyallRun.Status.CANCELLED, 0, Map.of());
            runs.put(tx, runId.toString(), cancelled, current.revision()); return cancelled;
        });
    }

    @Override public CompletionStage<Optional<KeyallRun>> find(UUID runId) {
        return store.read(reader -> runs.get(reader, runId.toString()).map(RecordRepository.VersionedValue::value));
    }

    @Override public CompletionStage<Optional<KeyallRun>> contribute(String definitionId, long amount, Collection<UUID> recipients, String operationKey) {
        if (amount < 1) throw new IllegalArgumentException("Threshold contribution must be positive"); KeyallDefinition definition = require(definitionId);
        if (definition.threshold() < 1) throw new IllegalStateException("KEYALL_THRESHOLD_DISABLED");
        return store.transact("keyall-threshold:" + operationKey, tx -> {
            var current = thresholds.get(tx, definitionId); KeyallThreshold before = current.map(RecordRepository.VersionedValue::value)
                    .orElse(new KeyallThreshold(definitionId, 0, clock.instant()));
            if (!IdempotencyKeys.reserve(tx, "keyall-threshold", operationKey)) return false;
            long total = Math.addExact(before.progress(), amount); boolean triggered = total >= definition.threshold();
            KeyallThreshold after = new KeyallThreshold(definitionId, triggered ? total - definition.threshold() : total, clock.instant());
            thresholds.put(tx, definitionId, after, current.map(RecordRepository.VersionedValue::revision).orElse(0L)); return triggered;
        }).thenCompose(triggered -> {
            if (!triggered) return CompletableFuture.completedFuture(Optional.empty());
            return preview(definitionId, KeyallRun.Trigger.THRESHOLD, recipients).thenCompose(run -> execute(run.id(), "threshold:" + run.id())).thenApply(Optional::of);
        });
    }

    @Override public CompletionStage<Integer> recover() {
        return store.read(reader -> {
            ArrayList<KeyallRun> pending = new ArrayList<>(); String after = null;
            while (true) { var page = runs.scanPage(reader, after, 1000); page.stream().map(RecordRepository.KeyedVersionedValue::value)
                    .filter(value -> value.status() == KeyallRun.Status.RUNNING).forEach(pending::add);
                if (page.size() < 1000) break; after = page.getLast().key(); }
            return pending;
        }).thenCompose(pending -> {
            CompletableFuture<?>[] tasks = pending.stream().map(run -> deliver(run, 0).toCompletableFuture()).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(tasks).thenApply(ignored -> pending.size());
        });
    }

    private List<KeyallDelivery> scanDeliveries(com.magicstudios.magiccore.storage.DataReader reader, UUID runId) throws Exception {
        ArrayList<KeyallDelivery> result = new ArrayList<>(); String prefix = runId + ":"; String after = null;
        while (true) { var page = deliveries.scanPage(reader, after, 1000); page.stream().filter(value -> value.key().startsWith(prefix)).map(RecordRepository.KeyedVersionedValue::value).forEach(result::add);
            if (page.size() < 1000) break; after = page.getLast().key(); }
        return result;
    }
    private KeyallDefinition require(String id) { KeyallDefinition value = definitions.get(id); if (value == null) throw new IllegalArgumentException("Unknown keyall " + id); return value; }
    private KeyallRun copy(KeyallRun run, KeyallRun.Status status, int delivered, Map<UUID,String> failures) { return new KeyallRun(run.id(), run.definitionId(), run.trigger(), status, run.recipients(), delivered, failures, run.createdAt(), clock.instant()); }
    private static Throwable root(Throwable failure) { Throwable value = failure; while (value.getCause() != null) value = value.getCause(); return value; }
}
