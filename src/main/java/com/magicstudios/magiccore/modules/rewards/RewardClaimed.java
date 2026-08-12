package com.magicstudios.magiccore.modules.rewards;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RewardClaimed(UUID playerId, String rewardId, String claimType,
                            String operationKey, Instant occurredAt) implements DomainEvent {
}
