package com.magicstudios.magiccore.integrations.vault;

import java.time.Instant;
import java.util.UUID;

public record VaultSaga(String operationKey, String type, UUID fromPlayer, UUID toPlayer,
                        long amountMinor, String state, String detail,
                        long resultingBalanceMinor, Instant updatedAt) {
}
