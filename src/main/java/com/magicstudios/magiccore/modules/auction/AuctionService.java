package com.magicstudios.magiccore.modules.auction;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface AuctionService {
    CompletionStage<AuctionMutation> create(UUID sellerId, String category, ItemFingerprint fingerprint,
                                            int quantity, long priceMinor, Duration duration, String operationKey);
    CompletionStage<AuctionMutation> purchase(UUID buyerId, UUID listingId, String operationKey);
    CompletionStage<AuctionMutation> cancel(UUID sellerId, UUID listingId, String operationKey);
    CompletionStage<Integer> expire(String operationKey, int limit);
    CompletionStage<AuctionPage> search(String query, String category, Sort sort, int page, int pageSize);
    CompletionStage<List<AuctionListing>> history(UUID playerId, int limit);
    enum Sort { NEWEST, PRICE_ASC, PRICE_DESC, EXPIRING }
}
