package com.magicstudios.magiccore.admin;

import java.time.Instant;
import java.util.UUID;

public record InputSession(UUID id, UUID playerId, String field, Instant expiresAt, String submittedInput) {
    public boolean completed() {
        return submittedInput != null;
    }
}
