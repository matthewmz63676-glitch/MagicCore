package com.magicstudios.magiccore.config.model;

public record SecureStorageFile(int configVersion, long leaseSeconds, int maximumVaults, String vaultRowsLimit,
                                int maximumItemPayloadBytes, int maximumContainerPayloadBytes,
                                String nestedContainerPolicy, String customItemPolicy, String adminCapability) { }
