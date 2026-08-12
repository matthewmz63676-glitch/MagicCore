package com.magicstudios.magiccore.modules.worth;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import java.util.Set;

public record ValuationInput(String itemId, String materialId, ItemFingerprint fingerprint,
                             int amount, int enchantmentLevels, int damage, int maximumDamage,
                             boolean nonstandardMetadata, int containedItemCount,
                             String spawnerEntityId, Set<String> metadataKeys) {
    public ValuationInput {
        if(itemId==null||itemId.isBlank()||materialId==null||materialId.isBlank())throw new IllegalArgumentException("item IDs are required");
        if(amount<1||amount>99_999||enchantmentLevels<0||damage<0||maximumDamage<0||containedItemCount<0)throw new IllegalArgumentException("valuation counters are invalid");
        metadataKeys=Set.copyOf(metadataKeys);spawnerEntityId=spawnerEntityId==null?"":spawnerEntityId;
    }
}
