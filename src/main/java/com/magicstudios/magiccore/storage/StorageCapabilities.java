package com.magicstudios.magiccore.storage;

public record StorageCapabilities(boolean atomicTransactions, boolean compareAndSwap,
                                  boolean durableUniqueKeys, boolean networked) {
    public void requireCriticalTransactions(String providerName) {
        if (!atomicTransactions || !compareAndSwap || !durableUniqueKeys) {
            throw new StorageCapabilityException(providerName
                    + " cannot guarantee critical MagicCore transactions. Use a transaction-capable deployment or SQLITE/MARIADB.");
        }
    }
}
