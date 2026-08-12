package com.magicstudios.magiccore.modules.resets;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.audit.AuditEvent;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.modules.statistics.PlayerStats;
import com.magicstudios.magiccore.modules.statistics.StatsChanged;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentResetAdminService implements ResetAdminService {
    private static final String STATISTICS = "STATISTICS";
    private final TransactionalDataStore store;
    private final AuditService audit;
    private final DomainEventBus events;
    private final Clock clock;
    private final Duration confirmationTtl;
    private final RecordRepository<ResetJob> jobs = new RecordRepository<>("resets.job", ResetJob.class);
    private final RecordRepository<PlayerStats> stats = new RecordRepository<>("statistics.player", PlayerStats.class);
    private final RecordRepository<StatsResetBackup> backups = new RecordRepository<>("resets.statistics-backup", StatsResetBackup.class);

    public PersistentResetAdminService(TransactionalDataStore store, AuditService audit, DomainEventBus events,
                                       Clock clock, Duration confirmationTtl) {
        this.store = store; this.audit = audit; this.events = events; this.clock = clock; this.confirmationTtl = confirmationTtl;
    }

    @Override public Set<String> supportedScopes() { return Set.of(STATISTICS); }

    @Override public CompletionStage<ResetJob> previewPlayer(UUID actorId, UUID playerId, Set<String> scopes) {
        Set<String> normalized = normalize(scopes);
        return store.transact("reset-preview-player:" + UUID.randomUUID(), tx -> {
            long count = stats.get(tx, playerId.toString()).isPresent() ? 1 : 0;
            ResetJob job = job(actorId, ResetJob.Target.PLAYER, playerId, normalized, Set.of(), 1, count);
            jobs.put(tx, job.id().toString(), job, 0); return job;
        });
    }

    @Override public CompletionStage<ResetJob> previewServer(UUID actorId, Set<String> scopes, Set<UUID> exclusions, int batchSize) {
        if (batchSize < 1 || batchSize > 1000) throw new IllegalArgumentException("Reset batch size must be 1..1000");
        Set<String> normalized = normalize(scopes); Set<UUID> excluded = Set.copyOf(exclusions);
        return store.transact("reset-preview-server:" + UUID.randomUUID(), tx -> {
            long count = 0; String after = null;
            while (true) {
                var page = stats.scanPage(tx, after, 1000);
                count += page.stream().filter(value -> !excluded.contains(value.value().playerId())).count();
                if (page.size() < 1000) break; after = page.getLast().key();
            }
            ResetJob job = job(actorId, ResetJob.Target.SERVER, null, normalized, excluded, batchSize, count);
            jobs.put(tx, job.id().toString(), job, 0); return job;
        });
    }

    @Override public CompletionStage<ResetJob> confirm(UUID resetId, String confirmationToken, String operationKey) {
        return store.transact("reset-confirm:" + operationKey, tx -> {
            var current = jobs.get(tx, resetId.toString()).orElseThrow(() -> new IllegalArgumentException("RESET_NOT_FOUND"));
            ResetJob job = current.value();
            if (job.status() == ResetJob.Status.COMPLETE) return job;
            if (!IdempotencyKeys.reserve(tx, "reset-confirm", operationKey)) return job;
            if (job.status() != ResetJob.Status.PREVIEWED || job.expiresAt().isBefore(clock.instant())) throw new IllegalStateException("RESET_PREVIEW_EXPIRED");
            if (!job.confirmationToken().equals(confirmationToken)) throw new SecurityException("RESET_CONFIRMATION_MISMATCH");
            ResetJob running = copy(job, 0, null, ResetJob.Status.RUNNING);
            jobs.put(tx, resetId.toString(), running, current.revision()); return running;
        }).thenCompose(this::process);
    }

    @Override public CompletionStage<ResetJob> resume(UUID resetId, String operationKey) {
        return store.transact("reset-resume:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "reset-resume", operationKey)) return jobs.get(tx, resetId.toString()).orElseThrow().value();
            ResetJob job = jobs.get(tx, resetId.toString()).orElseThrow(() -> new IllegalArgumentException("RESET_NOT_FOUND")).value();
            if (job.status() != ResetJob.Status.RUNNING && job.status() != ResetJob.Status.COMPLETE) throw new IllegalStateException("RESET_NOT_RESUMABLE");
            return job;
        }).thenCompose(job -> job.status() == ResetJob.Status.COMPLETE ? CompletableFuture.completedFuture(job) : process(job));
    }

    @Override public CompletionStage<Optional<ResetJob>> find(UUID resetId) {
        return store.read(reader -> jobs.get(reader, resetId.toString()).map(RecordRepository.VersionedValue::value));
    }

    private CompletionStage<ResetJob> process(ResetJob job) {
        return processBatch(job).thenCompose(outcome -> {
            outcome.changed().forEach(value -> events.publish(new StatsChanged(value.playerId(), value,
                    "reset:" + job.id(), clock.instant())));
            if (outcome.job().status() == ResetJob.Status.COMPLETE) return audit(outcome.job()).thenApply(ignored -> outcome.job());
            return process(outcome.job());
        });
    }

    private CompletionStage<BatchOutcome> processBatch(ResetJob job) {
        return store.transact("reset-batch:" + job.id() + ":" + (job.checkpoint() == null ? "start" : job.checkpoint()), tx -> {
            var current = jobs.get(tx, job.id().toString()).orElseThrow(); ResetJob active = current.value();
            if (active.status() == ResetJob.Status.COMPLETE) return new BatchOutcome(active, List.of());
            List<RecordRepository.KeyedVersionedValue<PlayerStats>> page;
            if (active.target() == ResetJob.Target.PLAYER) {
                var value = stats.get(tx, active.playerId().toString());
                page = value.map(record -> List.of(new RecordRepository.KeyedVersionedValue<>(active.playerId().toString(), record.value(), record.revision()))).orElse(List.of());
            } else page = stats.scanPage(tx, active.checkpoint(), active.batchSize());
            List<PlayerStats> changed = new ArrayList<>();
            for (var record : page) {
                UUID playerId = record.value().playerId(); if (active.exclusions().contains(playerId)) continue;
                String backupKey = active.id() + ":" + playerId;
                if (backups.get(tx, backupKey).isEmpty()) backups.put(tx, backupKey,
                        new StatsResetBackup(active.id(), playerId, record.value(), clock.instant()), 0);
                PlayerStats reset = new PlayerStats(playerId, 0, 0, 0, clock.instant());
                stats.put(tx, record.key(), reset, record.revision()); changed.add(reset);
            }
            boolean done = active.target() == ResetJob.Target.PLAYER || page.size() < active.batchSize();
            String checkpoint = page.isEmpty() ? active.checkpoint() : page.getLast().key();
            ResetJob updated = copy(active, Math.addExact(active.processedRecords(), changed.size()), checkpoint,
                    done ? ResetJob.Status.COMPLETE : ResetJob.Status.RUNNING);
            jobs.put(tx, active.id().toString(), updated, current.revision()); return new BatchOutcome(updated, List.copyOf(changed));
        });
    }

    private CompletionStage<Boolean> audit(ResetJob job) {
        return audit.record(new AuditEvent(UUID.randomUUID(), "reset:" + job.id(), "STATS_RESET", job.actorId().toString(),
                job.target() == ResetJob.Target.PLAYER ? job.playerId().toString() : "SERVER",
                Map.of("estimated", Long.toString(job.estimatedRecords()), "scopes", job.scopes().toString()),
                Map.of("processed", Long.toString(job.processedRecords()), "status", job.status().name()),
                "MagicCore:ResetAdmin", clock.instant()));
    }

    private ResetJob job(UUID actor, ResetJob.Target target, UUID player, Set<String> scopes, Set<UUID> exclusions,
                         int batchSize, long estimated) {
        UUID id = UUID.randomUUID(); var now = clock.instant();
        return new ResetJob(id, actor, target, player, scopes, exclusions, batchSize, estimated, 0, null,
                "RESET-" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT), ResetJob.Status.PREVIEWED,
                now, now.plus(confirmationTtl), now);
    }

    private Set<String> normalize(Set<String> requested) {
        if (requested == null || requested.isEmpty()) throw new IllegalArgumentException("At least one reset scope is required");
        Set<String> values = requested.stream().map(value -> value.toUpperCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (values.contains("ALL")) return supportedScopes();
        if (!supportedScopes().containsAll(values)) throw new IllegalArgumentException("Unsupported reset scope: " + values);
        return values;
    }

    private ResetJob copy(ResetJob job, long processed, String checkpoint, ResetJob.Status status) {
        return new ResetJob(job.id(), job.actorId(), job.target(), job.playerId(), job.scopes(), job.exclusions(), job.batchSize(),
                job.estimatedRecords(), processed, checkpoint, job.confirmationToken(), status, job.createdAt(), job.expiresAt(), clock.instant());
    }
    private record BatchOutcome(ResetJob job, List<PlayerStats> changed) { }
}
