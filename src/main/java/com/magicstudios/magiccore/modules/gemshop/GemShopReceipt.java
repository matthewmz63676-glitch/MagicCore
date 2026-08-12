package com.magicstudios.magiccore.modules.gemshop;

import java.time.Instant;
import java.util.UUID;

public record GemShopReceipt(UUID id, UUID quoteId, UUID playerId, String productId, long chargedMinor,
                             long balanceAfterMinor, UUID economyTransactionId, UUID deliveryId, Instant purchasedAt) { }
