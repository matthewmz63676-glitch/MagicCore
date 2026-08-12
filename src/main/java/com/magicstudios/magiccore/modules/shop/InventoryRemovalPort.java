package com.magicstudios.magiccore.modules.shop;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Implementations must re-read and remove only the exact fingerprint and quantity on the player's entity thread. */
public interface InventoryRemovalPort {
    CompletionStage<RemovalReceipt> removeExact(UUID playerId, ItemFingerprint fingerprint, int quantity, String operationKey);
    record RemovalReceipt(boolean removed, String code, String recoveryPayloadBase64) { }
}
