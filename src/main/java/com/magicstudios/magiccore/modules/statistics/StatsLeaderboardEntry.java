package com.magicstudios.magiccore.modules.statistics;

import java.util.UUID;

public record StatsLeaderboardEntry(int position, UUID playerId, long value) { }
