package com.magicstudios.magiccore.modules.statistics;

import java.time.Instant;
import java.util.UUID;

public record PlayerStats(UUID playerId, long kills, long deaths, long playtimeSeconds, Instant updatedAt) { }
