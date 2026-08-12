package com.magicstudios.magiccore.modules.events;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record KothRun(UUID id,String definitionId,Status status,String holdingGroup,String holdingName,long capturedMillis,
                      String winnerGroup,String winnerName,Set<UUID>winnerRecipients,Instant startedAt,Instant updatedAt,Instant completedAt){
    public enum Status{ACTIVE,COMPLETED,CANCELLED}
    public KothRun{winnerRecipients=Set.copyOf(winnerRecipients);}
    public static KothRun active(UUID id,String definitionId,Instant now){return new KothRun(id,definitionId,Status.ACTIVE,"","",0,"","",Set.of(),now,now,null);}
}
