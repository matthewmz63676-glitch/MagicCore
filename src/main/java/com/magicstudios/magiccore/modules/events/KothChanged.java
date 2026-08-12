package com.magicstudios.magiccore.modules.events;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record KothChanged(UUID runId,String definitionId,String status,String holder,String winner,Instant occurredAt)implements DomainEvent{}
