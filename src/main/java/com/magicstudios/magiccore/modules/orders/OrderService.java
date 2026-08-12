package com.magicstudios.magiccore.modules.orders;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface OrderService {
    CompletionStage<OrderMutation> create(UUID buyerId, String category, ItemFingerprint fingerprint,
                                          int quantity, long unitPriceMinor, Duration duration, String operationKey);
    CompletionStage<OrderMutation> fulfill(UUID sellerId, UUID orderId, int quantity, String operationKey);
    CompletionStage<OrderMutation> cancel(UUID buyerId, UUID orderId, String operationKey);
    CompletionStage<Integer> expire(String operationKey, int limit);
    CompletionStage<List<BuyOrder>> open(String category, int limit);
    CompletionStage<List<BuyOrder>> history(UUID playerId, int limit);
}
