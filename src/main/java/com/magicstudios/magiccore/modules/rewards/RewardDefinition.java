package com.magicstudios.magiccore.modules.rewards;

import java.util.Objects;

public record RewardDefinition(String id, String display, String rarity, int weight,
                               String currency, long amountMinor) {
    public RewardDefinition {
        id = Objects.requireNonNull(id, "id");
        if (!id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Reward ID must be upper snake case");
        display = Objects.requireNonNull(display, "display");
        rarity = Objects.requireNonNull(rarity, "rarity");
        if (weight < 1) throw new IllegalArgumentException("Reward weight must be positive");
        currency = Objects.requireNonNull(currency, "currency");
        if (amountMinor < 0) throw new IllegalArgumentException("Reward amount cannot be negative");
    }
}
