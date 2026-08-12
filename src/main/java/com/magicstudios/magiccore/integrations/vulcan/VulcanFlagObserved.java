package com.magicstudios.magiccore.integrations.vulcan;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record VulcanFlagObserved(UUID playerId,VulcanFlag flag,Instant occurredAt) implements DomainEvent { }
