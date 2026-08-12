package com.magicstudios.magiccore.modules.statistics;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentPlayerStatsService implements PlayerStatsService {
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final Clock clock;
    private final RecordRepository<PlayerStats> records = new RecordRepository<>("statistics.player", PlayerStats.class);

    public PersistentPlayerStatsService(TransactionalDataStore store, DomainEventBus events, Clock clock) {
        this.store = store; this.events = events; this.clock = clock;
    }

    @Override public CompletionStage<PlayerStats> stats(UUID playerId) { return store.read(reader -> load(reader, playerId)); }

    @Override public CompletionStage<PlayerStats> recordKill(VerifiedPlayerKill kill, String operationKey) {
        return store.transact("stats-kill:" + operationKey, transaction -> {
            PlayerStats killer = load(transaction, kill.killerId());
            if (!IdempotencyKeys.reserve(transaction, "stats-kill", kill.eventId().toString())) return killer;
            PlayerStats victim = load(transaction, kill.victimId());
            PlayerStats updatedKiller = new PlayerStats(killer.playerId(), killer.kills() + 1, killer.deaths(), killer.playtimeSeconds(), clock.instant());
            PlayerStats updatedVictim = new PlayerStats(victim.playerId(), victim.kills(), victim.deaths() + 1, victim.playtimeSeconds(), clock.instant());
            put(transaction, updatedKiller); put(transaction, updatedVictim); return updatedKiller;
        }).thenApply(result -> { events.publish(new StatsChanged(result.playerId(), result, operationKey, clock.instant())); return result; });
    }

    @Override public CompletionStage<PlayerStats> recordDeath(UUID playerId, UUID deathEventId, String operationKey) {
        return store.transact("stats-death:" + operationKey, transaction -> {
            PlayerStats before = load(transaction, playerId);
            if (!IdempotencyKeys.reserve(transaction, "stats-death", deathEventId.toString())) return before;
            PlayerStats updated = new PlayerStats(playerId, before.kills(), before.deaths() + 1, before.playtimeSeconds(), clock.instant());
            put(transaction, updated); return updated;
        }).thenApply(result -> { events.publish(new StatsChanged(playerId, result, operationKey, clock.instant())); return result; });
    }

    @Override public CompletionStage<PlayerStats> addPlaytime(UUID playerId, long seconds, String operationKey) {
        if (seconds < 0) throw new IllegalArgumentException("playtime seconds must not be negative");
        return store.transact("stats-playtime:" + operationKey, transaction -> {
            PlayerStats before = load(transaction, playerId);
            if (!IdempotencyKeys.reserve(transaction, "stats-playtime", operationKey)) return before;
            PlayerStats updated = new PlayerStats(playerId, before.kills(), before.deaths(),
                    Math.addExact(before.playtimeSeconds(), seconds), clock.instant());
            put(transaction, updated); return updated;
        }).thenApply(result -> { events.publish(new StatsChanged(playerId, result, operationKey, clock.instant())); return result; });
    }

    @Override public CompletionStage<List<StatsLeaderboardEntry>> leaderboard(StatsMetric metric, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("leaderboard limit must be 1..100");
        return store.read(reader -> {
            var sorted = scanAll(reader).stream()
                    .sorted(Comparator.comparingLong((PlayerStats stats) -> value(stats, metric)).reversed()
                            .thenComparing(stats -> stats.playerId().toString())).limit(limit).toList();
            java.util.ArrayList<StatsLeaderboardEntry> result = new java.util.ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) result.add(new StatsLeaderboardEntry(i + 1, sorted.get(i).playerId(), value(sorted.get(i), metric)));
            return List.copyOf(result);
        });
    }

    private PlayerStats load(com.magicstudios.magiccore.storage.DataReader reader, UUID playerId) throws Exception {
        return records.get(reader, playerId.toString()).map(RecordRepository.VersionedValue::value)
                .orElse(new PlayerStats(playerId, 0, 0, 0, clock.instant()));
    }
    private List<PlayerStats> scanAll(com.magicstudios.magiccore.storage.DataReader reader)throws Exception{java.util.ArrayList<PlayerStats>all=new java.util.ArrayList<>();String after=null;
        while(true){var page=records.scanPage(reader,after,1000);page.forEach(value->all.add(value.value()));if(page.size()<1000)break;after=page.get(page.size()-1).key();}return all;}
    private void put(com.magicstudios.magiccore.storage.DataTransaction transaction, PlayerStats stats) throws Exception {
        var current = records.get(transaction, stats.playerId().toString());
        records.put(transaction, stats.playerId().toString(), stats, current.map(RecordRepository.VersionedValue::revision).orElse(0L));
    }
    private static long value(PlayerStats stats, StatsMetric metric) { return switch (metric) {
        case KILLS -> stats.kills(); case DEATHS -> stats.deaths(); case PLAYTIME -> stats.playtimeSeconds();
    }; }
}
