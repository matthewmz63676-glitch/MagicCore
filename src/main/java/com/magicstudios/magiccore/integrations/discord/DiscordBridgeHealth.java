package com.magicstudios.magiccore.integrations.discord;

import java.time.Instant;

public record DiscordBridgeHealth(boolean available,long pending,long dead,Instant checkedAt) { }
