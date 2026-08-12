package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.admin.AdminActor;
import com.magicstudios.magiccore.admin.CapabilityGate;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.ranks.RankService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentWarpService implements WarpService {
    private final TransactionalDataStore store;
    private final RankService ranks;
    private final CapabilityService capabilities;
    private final CapabilityGate adminGate;
    private final Clock clock;
    private final int maximumNameLength;
    private final RecordRepository<ServerWarp> warps = new RecordRepository<>("essentials.warp", ServerWarp.class);

    public PersistentWarpService(TransactionalDataStore store, RankService ranks,
                                 CapabilityService capabilities, CapabilityGate adminGate,
                                 Clock clock, int maximumNameLength) {
        this.store = store;
        this.ranks = ranks;
        this.capabilities = capabilities;
        this.adminGate = adminGate;
        this.clock = clock;
        this.maximumNameLength = maximumNameLength;
    }

    @Override
    public CompletionStage<List<ServerWarp>> visibleWarps(UUID playerId) {
        return store.read(reader -> warps.scan(reader, null, 1000).stream()
                .map(RecordRepository.VersionedValue::value).toList())
                .thenCompose(all -> filterVisible(playerId, all));
    }

    @Override
    public CompletionStage<Optional<ServerWarp>> findVisible(UUID playerId, String warpId) {
        String id = normalize(warpId);
        return store.read(reader -> warps.get(reader, id).map(RecordRepository.VersionedValue::value))
                .thenCompose(warp -> warp.<CompletionStage<Optional<ServerWarp>>>map(value ->
                        canUse(playerId, value).thenApply(allowed -> allowed ? Optional.of(value) : Optional.empty()))
                        .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    @Override
    public CompletionStage<WarpMutation> set(AdminActor actor, String name, WorldPosition position,
                                             WarpAccess access, String operationKey) {
        String id = normalize(name);
        return adminGate.has(actor, "MANAGE_MODULES").thenCompose(allowed -> {
            if (!allowed) throw new SecurityException("MANAGE_MODULES is required at mutation time");
            return store.transact("warp-set:" + operationKey, tx -> {
                var current = warps.get(tx, id);
                if (!IdempotencyKeys.reserve(tx, "warp", operationKey)) {
                    return new WarpMutation(false, "REPLAY", current.map(RecordRepository.VersionedValue::value).orElse(null));
                }
                ServerWarp updated = new ServerWarp(id, name, position, access, clock.instant(), actor.displayName());
                warps.put(tx, id, updated, current.map(RecordRepository.VersionedValue::revision).orElse(0L));
                return new WarpMutation(true, current.isEmpty() ? "CREATED" : "UPDATED", updated);
            });
        });
    }

    @Override
    public CompletionStage<WarpMutation> delete(AdminActor actor, String name, String operationKey) {
        String id = normalize(name);
        return adminGate.has(actor, "MANAGE_MODULES").thenCompose(allowed -> {
            if (!allowed) throw new SecurityException("MANAGE_MODULES is required at mutation time");
            return store.transact("warp-delete:" + operationKey, tx -> {
                var current = warps.get(tx, id);
                if (!IdempotencyKeys.reserve(tx, "warp", operationKey)) {
                    return new WarpMutation(false, "REPLAY", current.map(RecordRepository.VersionedValue::value).orElse(null));
                }
                if (current.isEmpty()) return new WarpMutation(false, "NOT_FOUND", null);
                tx.delete("essentials.warp", id, current.get().revision());
                return new WarpMutation(true, "DELETED", current.get().value());
            });
        });
    }

    private CompletionStage<List<ServerWarp>> filterVisible(UUID playerId, List<ServerWarp> all) {
        List<CompletableFuture<Optional<ServerWarp>>> checks = all.stream()
                .map(warp -> canUse(playerId, warp).thenApply(allowed -> allowed ? Optional.of(warp) : Optional.<ServerWarp>empty())
                        .toCompletableFuture()).toList();
        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(ignored ->
                checks.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
    }

    private CompletionStage<Boolean> canUse(UUID playerId, ServerWarp warp) {
        if (warp.access().publicAccess()) return CompletableFuture.completedFuture(true);
        CompletionStage<Boolean> rankAllowed = ranks.rankOf(playerId)
                .thenApply(rank -> warp.access().ranks().contains(rank));
        List<CompletableFuture<Boolean>> capabilityChecks = warp.access().capabilities().stream()
                .map(capability -> capabilities.has(playerId, capability).toCompletableFuture()).toList();
        return CompletableFuture.allOf(capabilityChecks.toArray(CompletableFuture[]::new))
                .thenCombine(rankAllowed, (ignored, byRank) -> byRank || capabilityChecks.stream().anyMatch(CompletableFuture::join));
    }

    private String normalize(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1," + maximumNameLength + "}")) {
            throw new IllegalArgumentException("Warp name must contain 1.." + maximumNameLength + " safe characters");
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
