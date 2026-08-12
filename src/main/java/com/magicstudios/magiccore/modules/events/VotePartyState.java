package com.magicstudios.magiccore.modules.events;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record VotePartyState(UUID cycleId,int count,Set<UUID>eligibleVoters,Instant updatedAt){public VotePartyState{eligibleVoters=Set.copyOf(eligibleVoters);}public static VotePartyState initial(Instant now){return new VotePartyState(UUID.randomUUID(),0,Set.of(),now);}}
