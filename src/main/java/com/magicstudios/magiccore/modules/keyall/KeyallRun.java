package com.magicstudios.magiccore.modules.keyall;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KeyallRun(UUID id, String definitionId, Trigger trigger, Status status, List<UUID> recipients,
                        int delivered, Map<UUID,String> failures, Instant createdAt, Instant updatedAt) {
    public enum Trigger { MANUAL, SCHEDULE, THRESHOLD }
    public enum Status { PREVIEWED, RUNNING, COMPLETE, PARTIAL, CANCELLED }
    public KeyallRun { recipients = List.copyOf(recipients); failures = Map.copyOf(failures); }
}
