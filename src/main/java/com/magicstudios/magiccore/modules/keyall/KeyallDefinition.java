package com.magicstudios.magiccore.modules.keyall;

import java.time.Duration;

public record KeyallDefinition(String id, String keyId, long amount, Audience audience, boolean offlineDelivery,
                               Duration scheduleInterval, long threshold) {
    public enum Audience { ONLINE, ALL_KNOWN }
    public KeyallDefinition {
        if (id == null || id.isBlank() || keyId == null || keyId.isBlank() || amount < 1
                || scheduleInterval.isNegative() || threshold < 0) throw new IllegalArgumentException("Invalid keyall definition");
    }
}
