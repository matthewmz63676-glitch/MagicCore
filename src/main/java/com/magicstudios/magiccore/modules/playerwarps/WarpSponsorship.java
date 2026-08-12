package com.magicstudios.magiccore.modules.playerwarps;

import java.time.Instant;
import java.util.UUID;

public record WarpSponsorship(UUID id,String warpId,UUID sponsorId,String currency,long chargedMinor,long refundedMinor,
                              Instant startsAt,Instant endsAt,Status status,Instant updatedAt){
 public enum Status{ACTIVE,EXPIRED,CANCELLED}
 public boolean activeAt(Instant now){return status==Status.ACTIVE&&endsAt.isAfter(now);}
}
