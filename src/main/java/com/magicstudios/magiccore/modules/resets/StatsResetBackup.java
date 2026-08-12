package com.magicstudios.magiccore.modules.resets;

import com.magicstudios.magiccore.modules.statistics.PlayerStats;
import java.time.Instant;
import java.util.UUID;

public record StatsResetBackup(UUID resetId, UUID playerId, PlayerStats before, Instant backedUpAt) { }
