package com.magicstudios.magiccore.modules.keyall;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record KeyallCompleted(UUID runId, String definitionId, int delivered, int failures,
                              Instant occurredAt) implements DomainEvent { }
