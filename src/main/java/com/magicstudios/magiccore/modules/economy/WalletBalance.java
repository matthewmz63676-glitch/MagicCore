package com.magicstudios.magiccore.modules.economy;

import java.util.UUID;

public record WalletBalance(UUID playerId, String currency, long minorUnits) {
}
