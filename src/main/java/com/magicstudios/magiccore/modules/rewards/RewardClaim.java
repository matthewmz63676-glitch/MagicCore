package com.magicstudios.magiccore.modules.rewards;

import java.time.Instant;
import java.util.UUID;

public record RewardClaim(UUID id, String operationKey, UUID playerId, String claimType,
                          String rewardId, String rewardDisplay, String currency,
                          long amountMinor, Instant claimedAt) {
}
