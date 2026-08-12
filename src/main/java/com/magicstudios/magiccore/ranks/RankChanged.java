package com.magicstudios.magiccore.ranks;

import com.magicstudios.magiccore.api.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record RankChanged(UUID playerId, String previousRank, String currentRank,
                          String actor, Instant occurredAt) implements DomainEvent {
}
