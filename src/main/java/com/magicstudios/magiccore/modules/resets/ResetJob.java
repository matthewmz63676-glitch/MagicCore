package com.magicstudios.magiccore.modules.resets;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ResetJob(UUID id, UUID actorId, Target target, UUID playerId, Set<String> scopes,
                       Set<UUID> exclusions, int batchSize, long estimatedRecords, long processedRecords,
                       String checkpoint, String confirmationToken, Status status,
                       Instant createdAt, Instant expiresAt, Instant updatedAt) {
    public enum Target { PLAYER, SERVER }
    public enum Status { PREVIEWED, RUNNING, COMPLETE, CANCELLED }
    public ResetJob {
        scopes = Set.copyOf(scopes); exclusions = Set.copyOf(exclusions);
    }
}
