package com.magicstudios.magiccore.modules.combat;
import java.time.Instant;
import java.util.UUID;
public record CombatTag(UUID tagId,UUID playerId,UUID opponentId,Instant startedAt,Instant expiresAt){public boolean activeAt(Instant now){return expiresAt.isAfter(now);}}
