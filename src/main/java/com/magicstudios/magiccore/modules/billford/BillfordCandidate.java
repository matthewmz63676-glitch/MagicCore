package com.magicstudios.magiccore.modules.billford;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
public record BillfordCandidate(String itemId,ItemFingerprint fingerprint,int amount,boolean protectedItem){public BillfordCandidate{if(amount<1)throw new IllegalArgumentException("amount must be positive");}}
