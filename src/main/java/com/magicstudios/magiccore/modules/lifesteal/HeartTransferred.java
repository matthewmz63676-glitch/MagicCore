package com.magicstudios.magiccore.modules.lifesteal;
import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;
public record HeartTransferred(UUID killerId,UUID victimId,int killerHearts,int victimHearts,boolean overflowItem,
                               String operationKey,Instant occurredAt)implements DomainEvent{}
