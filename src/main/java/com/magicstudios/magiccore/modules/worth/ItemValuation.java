package com.magicstudios.magiccore.modules.worth;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;

public record ItemValuation(boolean sellable, String code, String entryId, String itemId,
                            String category, ItemFingerprint fingerprint, int quantity,
                            long unitWorthMinor, long totalWorthMinor) { }
