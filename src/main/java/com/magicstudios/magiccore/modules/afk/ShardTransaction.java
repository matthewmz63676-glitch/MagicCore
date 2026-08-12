package com.magicstudios.magiccore.modules.afk;

import java.time.Instant;
import java.util.UUID;

public record ShardTransaction(UUID id,UUID playerId,long delta,long balanceAfter,String reason,
                               String operationKey,Instant occurredAt) { }
