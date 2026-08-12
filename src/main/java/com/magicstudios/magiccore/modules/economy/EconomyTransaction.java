package com.magicstudios.magiccore.modules.economy;

import java.time.Instant;
import java.util.UUID;

public record EconomyTransaction(UUID id, String operationKey, String type, String currency,
                                 UUID fromPlayer, UUID toPlayer, long amountMinor,
                                 long fromBefore, long fromAfter, long toBefore, long toAfter,
                                 String actor, String reason, Instant timestamp) {
}
