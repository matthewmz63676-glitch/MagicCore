package com.magicstudios.magiccore.modules.spawnstash;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface SpawnStashService {
    CompletionStage<SpawnStashCase> prepare(UUID caseId, UUID targetId, UUID actorId, String actorName,
                                            StashPosition origin, List<SpawnStashBlock> blocks,
                                            Instant expiresAt, String operationKey);
    CompletionStage<SpawnStashCase> markPlaced(UUID caseId, StashPosition position, String operationKey);
    CompletionStage<SpawnStashCase> activate(UUID caseId, String operationKey);
    CompletionStage<SpawnStashCase> recordSignal(UUID caseId, SpawnStashSignal.Type type, UUID playerId,
                                                 StashPosition position, Map<String, String> details,
                                                 String operationKey);
    CompletionStage<SpawnStashCase> addNote(UUID caseId, UUID actorId, String actorName, String note, String operationKey);
    CompletionStage<SpawnStashCase> beginCleanup(UUID caseId, SpawnStashCase.Outcome outcome,
                                                 UUID actorId, String actorName, String note, String operationKey);
    CompletionStage<SpawnStashCase> markRestored(UUID caseId, StashPosition position, String operationKey);
    CompletionStage<SpawnStashCase> completeCleanup(UUID caseId, String operationKey);
    CompletionStage<Optional<SpawnStashCase>> find(UUID caseId);
    CompletionStage<List<SpawnStashCase>> openCases();
    CompletionStage<List<SpawnStashCase>> activeForTarget(UUID targetId);
}
