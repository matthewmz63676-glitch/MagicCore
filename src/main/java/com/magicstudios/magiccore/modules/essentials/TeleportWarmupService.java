package com.magicstudios.magiccore.modules.essentials;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe state machine; the platform adapter owns actual region/entity scheduling. */
public final class TeleportWarmupService {
    private final ConcurrentHashMap<UUID, TeleportWarmup> warmups = new ConcurrentHashMap<>();
    private final Clock clock;
    private final double movementToleranceSquared;

    public TeleportWarmupService(Clock clock, double movementTolerance) {
        this.clock = clock;
        if (movementTolerance < 0) throw new IllegalArgumentException("movementTolerance cannot be negative");
        this.movementToleranceSquared = movementTolerance * movementTolerance;
    }

    public TeleportWarmup begin(UUID playerId, WorldPosition origin, WorldPosition destination,
                                Duration delay, String operationKey) {
        TeleportWarmup warmup = new TeleportWarmup(playerId, origin, destination, clock.instant().plus(delay), operationKey);
        warmups.put(playerId, warmup);
        return warmup;
    }

    public boolean observeMovement(UUID playerId, WorldPosition current) {
        TeleportWarmup warmup = warmups.get(playerId);
        if (warmup == null || warmup.origin().distanceSquared(current) <= movementToleranceSquared) return false;
        return warmups.remove(playerId, warmup);
    }

    public Optional<TeleportWarmup> takeReady(UUID playerId) {
        TeleportWarmup warmup = warmups.get(playerId);
        if (warmup == null || warmup.completesAt().isAfter(clock.instant())) return Optional.empty();
        return warmups.remove(playerId, warmup) ? Optional.of(warmup) : Optional.empty();
    }

    public boolean cancel(UUID playerId) { return warmups.remove(playerId) != null; }
}
