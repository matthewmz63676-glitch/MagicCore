package com.magicstudios.magiccore.modules.resets;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ResetAdminService {
    Set<String> supportedScopes();
    CompletionStage<ResetJob> previewPlayer(UUID actorId, UUID playerId, Set<String> scopes);
    CompletionStage<ResetJob> previewServer(UUID actorId, Set<String> scopes, Set<UUID> exclusions, int batchSize);
    CompletionStage<ResetJob> confirm(UUID resetId, String confirmationToken, String operationKey);
    CompletionStage<ResetJob> resume(UUID resetId, String operationKey);
    CompletionStage<Optional<ResetJob>> find(UUID resetId);
}
