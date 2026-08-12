package com.magicstudios.magiccore.modules.rewards;

import java.time.Duration;

public record RewardClaimResult(boolean applied, String code, RewardClaim claim, Duration retryAfter) {
    public static RewardClaimResult claimed(boolean applied, RewardClaim claim) {
        return new RewardClaimResult(applied, applied ? "CLAIMED" : "REPLAY", claim, Duration.ZERO);
    }

    public static RewardClaimResult unavailable(String code, Duration retryAfter) {
        return new RewardClaimResult(false, code, null, retryAfter);
    }
}
