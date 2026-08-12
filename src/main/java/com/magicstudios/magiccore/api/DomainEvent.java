package com.magicstudios.magiccore.api;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
