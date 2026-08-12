package com.magicstudios.magiccore.modules.shop;

import com.magicstudios.magiccore.modules.worth.ValuationInput;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface AdvancedSellService {
    CompletionStage<SellBatchQuote> quote(UUID playerId,SellScope scope,String category,List<ValuationInput> inventorySnapshot,String operationKey);
    CompletionStage<SellReceipt> execute(UUID playerId,UUID quoteId,String operationKey);
    CompletionStage<Optional<SellReceipt>> receipt(UUID receiptId);
    CompletionStage<List<SellReceipt>> history(UUID playerId,int limit);
    CompletionStage<Integer> recoverRemoved();
    CompletionStage<SellReceipt> reconcile(UUID quoteId,boolean removalConfirmed,String operationKey);
}
