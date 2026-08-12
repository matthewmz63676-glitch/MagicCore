package com.magicstudios.magiccore.modules.crates;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CrateOpening(UUID id, UUID playerId, String crateId, int amount,
                           List<GrantedCrateReward> rewards, long totalOpens, Instant openedAt) {
    public CrateOpening { rewards = List.copyOf(rewards); }
}
