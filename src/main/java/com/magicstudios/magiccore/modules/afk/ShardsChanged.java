package com.magicstudios.magiccore.modules.afk;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ShardsChanged(UUID playerId,long amount,long delta,String reason,String operationKey,
                            Instant occurredAt) implements DomainEvent { }
