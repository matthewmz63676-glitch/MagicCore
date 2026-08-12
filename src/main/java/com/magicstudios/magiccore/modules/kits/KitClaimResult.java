package com.magicstudios.magiccore.modules.kits;

import java.time.Instant;

public record KitClaimResult(boolean applied, String code, Instant nextAvailableAt) {
}
