package com.magicstudios.magiccore.modules.rewards;

import java.time.Instant;
import java.util.UUID;

public record DailyRewardState(UUID playerId, Instant lastClaimAt, int currentStreak, int bestStreak) {
}
