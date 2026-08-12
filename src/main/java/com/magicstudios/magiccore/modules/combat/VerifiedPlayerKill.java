package com.magicstudios.magiccore.modules.combat;
import java.time.Instant;
import java.util.UUID;
public record VerifiedPlayerKill(UUID eventId,UUID killerId,UUID victimId,String verifier,Instant occurredAt){
 public VerifiedPlayerKill{if(killerId.equals(victimId))throw new IllegalArgumentException("Self kill is not claimable");}
}
