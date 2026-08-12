package com.magicstudios.magiccore.modules.economy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface EconomyService {
    String primaryCurrency();

    Map<String, CurrencyDefinition> currencies();

    CompletionStage<Money> balance(UUID playerId, String currency);

    CompletionStage<EconomyMutation> transfer(UUID from, UUID to, Money amount, String operationKey);

    CompletionStage<EconomyMutation> adjust(UUID playerId, Money delta, String actor, String reason, String operationKey);

    CompletionStage<List<EconomyTransaction>> transactions(String afterKey, int limit);
}
