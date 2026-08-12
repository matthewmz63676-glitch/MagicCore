package com.magicstudios.magiccore.modules.shop;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Revalidates every line before performing one all-or-nothing inventory mutation. */
public interface BatchInventoryRemovalPort {
    CompletionStage<BatchRemovalReceipt> removeBatchExact(UUID playerId, List<RemovalLine> lines, String operationKey);
    record RemovalLine(ItemFingerprint fingerprint,int quantity) { public RemovalLine{if(quantity<1)throw new IllegalArgumentException("quantity must be positive");} }
    record BatchRemovalReceipt(boolean removed,String code,String recoveryPayloadBase64) { }
}
