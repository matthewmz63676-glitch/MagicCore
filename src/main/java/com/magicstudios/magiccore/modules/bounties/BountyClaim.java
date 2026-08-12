package com.magicstudios.magiccore.modules.bounties;
import java.time.Instant;
import java.util.UUID;
public record BountyClaim(UUID id,UUID killEventId,UUID killerId,UUID victimId,long payoutMinor,String currency,
                          String operationKey,String verifier,Instant claimedAt){}
