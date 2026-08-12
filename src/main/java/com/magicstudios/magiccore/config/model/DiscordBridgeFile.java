package com.magicstudios.magiccore.config.model;

public record DiscordBridgeFile(int configVersion, boolean enabled, String secretEnv, long linkCodeTtlSeconds,
                                long messageMaximumAgeSeconds, int maximumMessagesPerMinute,
                                int maximumRetryAttempts, long retryBaseSeconds, String bindHost, int bindPort) { }
