package com.magicstudios.magiccore.modules.gemshop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface GemShopService {
    String currency();
    List<GemProduct> products();
    List<GemProduct> products(String category);
    CompletionStage<GemShopQuote> quote(UUID playerId, String productId, String operationKey);
    CompletionStage<GemShopReceipt> confirm(UUID playerId, UUID quoteId, String operationKey);
    CompletionStage<Optional<GemShopReceipt>> receipt(UUID receiptId);
}
