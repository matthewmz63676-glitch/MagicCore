package com.magicstudios.magiccore.modules.shop;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ShopService {
    List<ShopProduct> products();
    CompletionStage<PurchaseResult> buy(UUID playerId, String productId, int quantity, String operationKey);
}
