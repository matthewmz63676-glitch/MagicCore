package com.magicstudios.magiccore.modules.teams;

import java.time.Instant;
import java.util.UUID;

public record TeamInvitation(UUID teamId, UUID invitedPlayerId, UUID invitedBy,
                             Instant createdAt, Instant expiresAt) {
    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
