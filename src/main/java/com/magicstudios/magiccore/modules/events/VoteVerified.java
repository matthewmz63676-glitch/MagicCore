package com.magicstudios.magiccore.modules.events;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record VoteVerified(UUID voteId,UUID playerId,String service,boolean counted,boolean partyTriggered,Instant occurredAt)implements DomainEvent{}
