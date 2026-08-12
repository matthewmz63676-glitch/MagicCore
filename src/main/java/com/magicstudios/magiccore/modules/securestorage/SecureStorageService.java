package com.magicstudios.magiccore.modules.securestorage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface SecureStorageService {
    CompletionStage<SecureStorageSession> open(UUID actorId, UUID ownerId, VirtualContainer.Type type, int containerIndex, String operationKey);
    CompletionStage<StorageCommit> save(UUID actorId, UUID leaseId, long expectedRevision, List<StoredItem> items, String operationKey);
    CompletionStage<Boolean> close(UUID actorId, UUID leaseId, String operationKey);
    CompletionStage<Integer> recoverExpired();
    CompletionStage<Boolean> enqueueRecovery(UUID ownerId, UUID leaseId, List<StoredItem> items, String operationKey);
}
