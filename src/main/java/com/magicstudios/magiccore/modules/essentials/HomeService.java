package com.magicstudios.magiccore.modules.essentials;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface HomeService {
    CompletionStage<List<Home>> homes(UUID ownerId);

    CompletionStage<Optional<Home>> findVisible(UUID viewerId, UUID ownerId, String homeId);

    CompletionStage<HomeMutation> set(UUID ownerId, String name, WorldPosition position, String operationKey);

    CompletionStage<HomeMutation> delete(UUID ownerId, String name, String operationKey);

    CompletionStage<HomeMutation> share(UUID ownerId, String name, UUID targetId, boolean shared,
                                        String operationKey);
}
