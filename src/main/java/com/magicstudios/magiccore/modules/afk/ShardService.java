package com.magicstudios.magiccore.modules.afk;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ShardService {
    CompletionStage<ShardBalance> balance(UUID playerId);
    CompletionStage<ShardAwardResult> award(UUID playerId,AfkEligibilitySnapshot eligibility,String intervalId);
    CompletionStage<ShardBalance> adjust(UUID playerId,long delta,String reason,String operationKey);
    CompletionStage<List<ShardTransaction>> history(UUID playerId,int limit);
}
