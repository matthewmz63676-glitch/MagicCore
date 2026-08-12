package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.admin.AdminActor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface WarpService {
    CompletionStage<List<ServerWarp>> visibleWarps(UUID playerId);

    CompletionStage<Optional<ServerWarp>> findVisible(UUID playerId, String warpId);

    CompletionStage<WarpMutation> set(AdminActor actor, String name, WorldPosition position,
                                      WarpAccess access, String operationKey);

    CompletionStage<WarpMutation> delete(AdminActor actor, String name, String operationKey);
}
