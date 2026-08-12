package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.crates.CrateCost;
import com.magicstudios.magiccore.modules.crates.CrateKeysChanged;
import com.magicstudios.magiccore.modules.crates.CrateOpened;
import com.magicstudios.magiccore.modules.crates.CrateReward;
import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.modules.statistics.PlayerStats;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.StatsChanged;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class PhaseFivePlaceholderView {
    private final CrateService crates;
    private final Set<String> keyIds;
    private final PlayerStatsService stats;
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public PhaseFivePlaceholderView(CrateService crates, PlayerStatsService stats) {
        this.crates = crates;
        this.stats = stats;
        Set<String> found = new LinkedHashSet<>();
        crates.definitions().values().forEach(crate -> {
            if (crate.cost().type() == CrateCost.Type.KEY) found.add(crate.cost().keyId());
            crate.rewards().stream().filter(reward -> reward.type() == CrateReward.Type.KEY).forEach(reward -> found.add(reward.keyId()));
        });
        this.keyIds = Set.copyOf(found);
    }

    public void register(String owner, PlaceholderRegistry registry, DomainEventBus events) {
        for (String keyId : keyIds) registry.register(owner, "crates_key_" + keyId.toLowerCase(),
                context -> value(context.subjectId(), keyId));
        registry.register(owner, "crates_definitions", context -> Integer.toString(crates.definitions().size()));
        events.subscribe(owner, CrateOpened.class, event -> refresh(event.playerId()));
        events.subscribe(owner, CrateKeysChanged.class, event -> refresh(event.playerId()));
        registry.register(owner,"stats_kills",context->stat(context.subjectId(),PlayerStats::kills));
        registry.register(owner,"stats_deaths",context->stat(context.subjectId(),PlayerStats::deaths));
        registry.register(owner,"stats_playtime_seconds",context->stat(context.subjectId(),PlayerStats::playtimeSeconds));
        events.subscribe(owner, StatsChanged.class, event -> statsCache.put(event.playerId(),event.stats()));
    }

    public CompletionStage<Void> refresh(UUID playerId) {
        var futures = keyIds.stream().map(keyId -> crates.keyBalance(playerId, keyId).toCompletableFuture()).toList();
        var statsFuture=stats.stats(playerId).toCompletableFuture();var all=new java.util.ArrayList<CompletableFuture<?>>();all.addAll(futures);all.add(statsFuture);
        return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).thenRun(() -> {
            Map<String, Long> values = new java.util.LinkedHashMap<>();
            futures.forEach(future -> values.put(future.join().keyId(), future.join().amount()));
            cache.put(playerId, Map.copyOf(values));
            statsCache.put(playerId,statsFuture.join());
        });
    }
    public void invalidate(UUID playerId) { cache.remove(playerId);statsCache.remove(playerId); }
    private String value(UUID playerId, String keyId) {
        Map<String, Long> values = playerId == null ? null : cache.get(playerId);
        return values == null ? "" : Long.toString(values.getOrDefault(keyId, 0L));
    }
    private String stat(UUID playerId,java.util.function.ToLongFunction<PlayerStats>getter){PlayerStats value=playerId==null?null:statsCache.get(playerId);return value==null?"":Long.toString(getter.applyAsLong(value));}
}
