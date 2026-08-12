package com.magicstudios.magiccore.modules.shop;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellBatchQuote(UUID id,UUID playerId,SellScope scope,String category,List<SellLine> lines,long creditMinor,
                             Status status,String executionKey,String recoveryPayloadBase64,Instant createdAt,Instant expiresAt,Instant updatedAt){
    public SellBatchQuote{lines=List.copyOf(lines);category=category==null?"":category;executionKey=executionKey==null?"":executionKey;recoveryPayloadBase64=recoveryPayloadBase64==null?"":recoveryPayloadBase64;}
    public enum Status{QUOTED,REMOVING,REMOVED,SETTLED,REJECTED,RECOVERY_REQUIRED}
}
