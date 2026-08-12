package com.magicstudios.magiccore.ranks;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface RankService {
    RankCatalog catalog();

    CompletionStage<String> rankOf(UUID playerId);

    CompletionStage<RankChange> setRank(UUID playerId, String rankId, String actor,
                                        String operationKey);

    CompletionStage<RankSyncPreview> previewSync(UUID playerId, String rankId);
}
