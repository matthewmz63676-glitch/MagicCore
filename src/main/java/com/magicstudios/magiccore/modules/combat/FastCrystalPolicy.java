package com.magicstudios.magiccore.modules.combat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FastCrystalPolicy {
    public enum Decision { ALLOWED, DISABLED, PLAYER_DISABLED, WORLD_BLOCKED, RANGE, LINE_OF_SIGHT, COOLDOWN }

    private final boolean enabled;
    private final Duration cooldown;
    private final double maximumRangeSquared;
    private final boolean requireLineOfSight;
    private final Set<String> worldAllowlist;
    private final ConcurrentHashMap<UUID, Instant> nextUse = new ConcurrentHashMap<>();

    public FastCrystalPolicy(boolean enabled, Duration cooldown, double maximumRange,
                             boolean requireLineOfSight, Set<String> worldAllowlist) {
        if (cooldown.isNegative() || maximumRange <= 0) throw new IllegalArgumentException("Invalid Fast Crystal policy");
        this.enabled = enabled;
        this.cooldown = cooldown;
        this.maximumRangeSquared = maximumRange * maximumRange;
        this.requireLineOfSight = requireLineOfSight;
        this.worldAllowlist = Set.copyOf(worldAllowlist);
    }

    public Decision evaluate(UUID playerId, boolean playerEnabled, String world, double distanceSquared,
                             boolean lineOfSight, Instant now) {
        if (!enabled) return Decision.DISABLED;
        if (!playerEnabled) return Decision.PLAYER_DISABLED;
        if (!worldAllowlist.isEmpty() && !worldAllowlist.contains(world)) return Decision.WORLD_BLOCKED;
        if (distanceSquared > maximumRangeSquared) return Decision.RANGE;
        if (requireLineOfSight && !lineOfSight) return Decision.LINE_OF_SIGHT;
        Instant blockedUntil = nextUse.get(playerId);
        if (blockedUntil != null && blockedUntil.isAfter(now)) return Decision.COOLDOWN;
        nextUse.put(playerId, now.plus(cooldown));
        return Decision.ALLOWED;
    }

    public Duration remaining(UUID playerId, Instant now) {
        Instant until = nextUse.get(playerId);
        return until == null || !until.isAfter(now) ? Duration.ZERO : Duration.between(now, until);
    }
}
