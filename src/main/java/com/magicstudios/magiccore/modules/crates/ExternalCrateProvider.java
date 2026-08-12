package com.magicstudios.magiccore.modules.crates;

import java.util.UUID;

public interface ExternalCrateProvider {
    String id();
    boolean available();
    boolean hasCrate(String crateId) throws Exception;
    long keyBalance(UUID playerId, String keyId) throws Exception;
    void grantKeys(UUID playerId, String keyId, long amount) throws Exception;
    boolean open(UUID playerId, String crateId) throws Exception;
}
