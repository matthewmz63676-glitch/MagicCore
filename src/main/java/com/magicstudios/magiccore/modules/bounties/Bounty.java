package com.magicstudios.magiccore.modules.bounties;
import java.time.Instant;
import java.util.UUID;
public record Bounty(UUID targetId,String currency,long totalEscrowMinor,int contributionCount,Status status,
                     UUID claimedBy,UUID claimEventId,Instant createdAt,Instant updatedAt,Instant claimedAt){
    public enum Status{ACTIVE,CLAIMED}
}
