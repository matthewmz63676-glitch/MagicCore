package com.magicstudios.magiccore.modules.crates;

public record CrateReward(String id, Type type, long weight, String rarity, String material, int amount,
                          String itemDataBase64, String currency, long amountMinor, String keyId, long keyAmount) {
    public enum Type { ITEM, CURRENCY, KEY }
    public CrateReward { if (weight < 1) throw new IllegalArgumentException("reward weight must be positive"); }
}
