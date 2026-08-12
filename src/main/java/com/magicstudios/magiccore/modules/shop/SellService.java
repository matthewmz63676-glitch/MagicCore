package com.magicstudios.magiccore.modules.shop;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface SellService {
    CompletionStage<SellQuote> quote(UUID playerId, String productId, ItemFingerprint fingerprint,
                                     int quantity, String operationKey);
    CompletionStage<SellResult> execute(UUID playerId, UUID quoteId, String operationKey);
}
