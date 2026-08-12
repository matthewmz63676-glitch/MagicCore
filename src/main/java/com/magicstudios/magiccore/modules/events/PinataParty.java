package com.magicstudios.magiccore.modules.events;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PinataParty(UUID id,Status status,int maximumHits,int totalHits,Map<UUID,Integer>playerHits,Set<UUID>eligibleVoters,
                          UUID finalHitter,Instant createdAt,Instant activatedAt,Instant completedAt){
    public enum Status{PENDING,ACTIVE,COMPLETED,CANCELLED}
    public PinataParty{playerHits=Map.copyOf(playerHits);eligibleVoters=Set.copyOf(eligibleVoters);}
    public int remaining(){return Math.max(0,maximumHits-totalHits);}
}
