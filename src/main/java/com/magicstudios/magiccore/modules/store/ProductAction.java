package com.magicstudios.magiccore.modules.store;

public record ProductAction(Type type, String currency, long amountMinor, String keyId, long keyAmount,
                            String material, int amount, String itemDataBase64, String rankId) {
    public enum Type { CURRENCY, CRATE_KEY, ITEM, RANK }
}
