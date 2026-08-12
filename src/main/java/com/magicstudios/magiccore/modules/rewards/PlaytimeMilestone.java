package com.magicstudios.magiccore.modules.rewards;

import java.util.Objects;

public record PlaytimeMilestone(String id, String display, long requiredMinutes,
                                String currency, long amountMinor) {
    public PlaytimeMilestone {
        id = Objects.requireNonNull(id, "id");
        if (!id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Milestone ID must be upper snake case");
        display = Objects.requireNonNull(display, "display");
        if (requiredMinutes < 1 || amountMinor < 0) throw new IllegalArgumentException("Milestone values are invalid");
        currency = Objects.requireNonNull(currency, "currency");
    }
}
