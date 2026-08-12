package com.magicstudios.magiccore.modules.events;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PinataChanged(UUID partyId,String status,int hits,int maximumHits,UUID actorId,Instant occurredAt)implements DomainEvent{}
