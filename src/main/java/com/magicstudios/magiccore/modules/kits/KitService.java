package com.magicstudios.magiccore.modules.kits;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface KitService {
    List<KitDefinition> definitions();
    CompletionStage<KitClaimResult> claim(UUID playerId, String kitId, String operationKey);
}
