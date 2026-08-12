package com.magicstudios.magiccore.modules.gemshop;

public record GemProduct(String id, String category, String displayName, String material, int amount,
                         String itemDataBase64, long priceMinor, String requiredCapability,
                         long minimumPlaytimeSeconds, long minimumKills) { }
