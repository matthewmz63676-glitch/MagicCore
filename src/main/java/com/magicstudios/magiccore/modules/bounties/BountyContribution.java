package com.magicstudios.magiccore.modules.bounties;
import java.time.Instant;
import java.util.UUID;
public record BountyContribution(UUID id,UUID targetId,UUID creatorId,long escrowMinor,long taxMinor,
                                 Status status,String operationKey,Instant createdAt,Instant closedAt){
    public enum Status{ACTIVE,CLAIMED}
}
