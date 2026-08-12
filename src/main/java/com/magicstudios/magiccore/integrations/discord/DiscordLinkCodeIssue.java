package com.magicstudios.magiccore.integrations.discord;

import java.time.Instant;
import java.util.UUID;

public record DiscordLinkCodeIssue(UUID playerId,String code,Instant expiresAt) { }
