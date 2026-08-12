package com.magicstudios.magiccore.modules.lifesteal;
import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;
public record PlayerEliminated(UUID playerId,UUID creditedKillerId,String operationKey,Instant occurredAt)implements DomainEvent{}
