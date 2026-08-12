package com.magicstudios.magiccore.config.model;

import java.util.List;

public record StoreFile(int configVersion, String url, boolean purchasesEnabled, String signatureSecretEnv,
                        long signatureMaximumAgeSeconds, boolean announcePurchases, DonationGoal donationGoal,
                        List<Product> products) {
    public StoreFile { products = List.copyOf(products); }
    public record DonationGoal(boolean enabled, long targetMinor) { }
    public record Product(String id, String displayName, long minimumPaidMinor, List<Action> actions) {
        public Product { actions = List.copyOf(actions); }
    }
    public record Action(String type, String currency, long amountMinor, String keyId, long keyAmount,
                         String material, int amount, String itemDataBase64, String rankId) { }
}
