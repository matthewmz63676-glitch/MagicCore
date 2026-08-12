package com.magicstudios.magiccore.modules.events;

import java.time.Instant;
import java.util.UUID;

public record VerifiedVote(UUID id,String providerEventId,UUID playerId,String service,boolean online,boolean counted,boolean rewardEligible,Instant occurredAt){}
