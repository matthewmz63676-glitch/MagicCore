package com.magicstudios.magiccore.integrations.npcs;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record NpcActionRequested(UUID playerId,String npcEntityId,NpcAction action,Instant occurredAt)implements DomainEvent{}
