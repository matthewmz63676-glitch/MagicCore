package com.magicstudios.magiccore.modules.economy;

import java.util.Objects;

public record CurrencyDefinition(String id, String display, String symbol, int decimalPlaces,
                                 long startingBalanceMinor, long maximumBalanceMinor) {
    public CurrencyDefinition {
        id = Objects.requireNonNull(id, "id");
        if (!id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Currency ID must be upper snake case");
        display = Objects.requireNonNull(display, "display");
        symbol = Objects.requireNonNull(symbol, "symbol");
        if (decimalPlaces < 0 || decimalPlaces > 6) throw new IllegalArgumentException("decimalPlaces must be 0..6");
        if (startingBalanceMinor < 0 || maximumBalanceMinor < startingBalanceMinor) {
            throw new IllegalArgumentException("Currency balance bounds are invalid");
        }
    }
}
