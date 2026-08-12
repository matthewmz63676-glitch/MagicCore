package com.magicstudios.magiccore.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record HealthReport(String component, HealthState state, String reason,
                           Map<String, String> details, Instant observedAt) {
    public HealthReport {
        component = Objects.requireNonNull(component, "component");
        state = Objects.requireNonNull(state, "state");
        reason = Objects.requireNonNull(reason, "reason");
        details = Map.copyOf(details);
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }

    public static HealthReport healthy(String component) {
        return new HealthReport(component, HealthState.HEALTHY, "healthy", Map.of(), Instant.now());
    }
}
