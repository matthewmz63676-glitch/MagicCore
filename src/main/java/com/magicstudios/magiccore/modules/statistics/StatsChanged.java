package com.magicstudios.magiccore.modules.statistics;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StatsChanged(UUID playerId, PlayerStats stats, String operationKey, Instant occurredAt) implements DomainEvent { }
