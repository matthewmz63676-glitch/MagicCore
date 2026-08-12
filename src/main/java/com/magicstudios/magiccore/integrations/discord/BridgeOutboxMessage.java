package com.magicstudios.magiccore.integrations.discord;

import java.time.Instant;
import java.util.UUID;

public record BridgeOutboxMessage(UUID id,BridgeEnvelope envelope,Status status,int attempts,String lastError,
                                  Instant nextAttemptAt,Instant createdAt,Instant updatedAt){
 public enum Status{PENDING,ACKNOWLEDGED,DEAD}
}
