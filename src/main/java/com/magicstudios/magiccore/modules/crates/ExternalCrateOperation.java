package com.magicstudios.magiccore.modules.crates;

import java.time.Instant;
import java.util.UUID;

public record ExternalCrateOperation(String operationKey, Type type, Status status, UUID playerId,
                                     String subjectId, long amount, CrateOpening opening,
                                     String detail, Instant createdAt, Instant updatedAt) {
    public enum Type { GRANT_KEYS, OPEN }
    public enum Status { PREPARED, COMPLETE, FAILED }
}
