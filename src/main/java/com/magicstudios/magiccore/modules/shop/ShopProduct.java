package com.magicstudios.magiccore.modules.shop;

public record ShopProduct(String id, String category, String material, int amount, long buyPriceMinor,
                          long sellPriceMinor, String itemDataBase64) {
    public ShopProduct {
        if (id == null || !id.matches("[a-z0-9_-]{1,48}")) throw new IllegalArgumentException("Invalid product ID");
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("Product amount must be 1..64");
        if (buyPriceMinor < 0 || sellPriceMinor < 0) throw new IllegalArgumentException("Prices cannot be negative");
    }
}
