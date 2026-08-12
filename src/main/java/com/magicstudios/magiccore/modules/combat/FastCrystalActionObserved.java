package com.magicstudios.magiccore.modules.combat;

import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record FastCrystalActionObserved(UUID playerId, String action, int recentVulcanFlags,
                                        Instant occurredAt) implements DomainEvent { }
