package com.magicstudios.magiccore.integrations.discord;

import java.time.Instant;
import java.util.UUID;

public record DiscordLink(UUID playerId, String discordId, Instant linkedAt, Instant revokedAt) {
    public boolean active(){return revokedAt==null;}
}
