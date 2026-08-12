package com.magicstudios.magiccore.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(UUID id, String operationKey, String action, String actor, String target,
                         Map<String, String> before, Map<String, String> after,
                         String source, Instant timestamp) {
    public AuditEvent {
        id = Objects.requireNonNull(id, "id");
        operationKey = Objects.requireNonNull(operationKey, "operationKey");
        action = Objects.requireNonNull(action, "action");
        actor = Objects.requireNonNull(actor, "actor");
        target = Objects.requireNonNull(target, "target");
        before = Map.copyOf(before);
        after = Map.copyOf(after);
        source = Objects.requireNonNull(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }
}
