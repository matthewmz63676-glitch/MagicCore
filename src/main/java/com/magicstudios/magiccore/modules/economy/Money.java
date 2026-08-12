package com.magicstudios.magiccore.modules.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(String currency, long minorUnits) {
    public Money {
        currency = Objects.requireNonNull(currency, "currency");
    }

    public BigDecimal decimal(CurrencyDefinition definition) {
        if (!definition.id().equals(currency)) throw new IllegalArgumentException("Currency mismatch");
        return BigDecimal.valueOf(minorUnits, definition.decimalPlaces());
    }

    public static Money exact(String currency, BigDecimal amount, CurrencyDefinition definition) {
        if (!definition.id().equals(currency)) throw new IllegalArgumentException("Currency mismatch");
        BigDecimal scaled = amount.setScale(definition.decimalPlaces(), RoundingMode.UNNECESSARY);
        return new Money(currency, scaled.movePointRight(definition.decimalPlaces()).longValueExact());
    }
}
