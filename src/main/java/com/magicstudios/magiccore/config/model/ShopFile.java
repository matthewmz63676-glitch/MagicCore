package com.magicstudios.magiccore.config.model;

import java.util.List;

public record ShopFile(int configVersion, String currency, List<Product> products, GemShop gemShop) {
    public ShopFile { products = List.copyOf(products); }
    public record Product(String id, String category, String material, int amount, long buyPriceMinor,
                          long sellPriceMinor, String itemDataBase64) { }
    public record GemShop(boolean enabled, String currency, long confirmationSeconds, List<GemProduct> products) {
        public GemShop { products = List.copyOf(products); }
    }
    public record GemProduct(String id, String category, String displayName, String material, int amount,
                             String itemDataBase64, long priceMinor, String requiredCapability,
                             long minimumPlaytimeSeconds, long minimumKills) { }
}
