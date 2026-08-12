package com.magicstudios.magiccore.modules.crates;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CrateService {
    Map<String, CrateDefinition> definitions();
    CompletionStage<CrateKeyBalance> keyBalance(UUID playerId, String keyId);
    CompletionStage<CrateKeyBalance> grantKeys(UUID playerId, String keyId, long amount, String operationKey);
    CompletionStage<CrateOpenResult> open(UUID playerId, String crateId, int amount, String operationKey);
    CompletionStage<List<CrateOpening>> history(UUID playerId, int limit);
}
