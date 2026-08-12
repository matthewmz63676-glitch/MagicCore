package com.magicstudios.magiccore.modules.shop;
public record SellLine(ItemFingerprint fingerprint,String itemId,String category,int quantity,long unitWorthMinor,long totalWorthMinor) { }
