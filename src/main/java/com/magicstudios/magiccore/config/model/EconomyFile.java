package com.magicstudios.magiccore.config.model;

import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public record EconomyFile(int configVersion, String primaryCurrency, Map<String, CurrencyEntry> currencies,
                          Payments payments, Analytics analytics) {
    public EconomyFile {
        currencies = Map.copyOf(currencies);
    }

    public Map<String, CurrencyDefinition> definitions() {
        Map<String, CurrencyDefinition> result = new LinkedHashMap<>();
        currencies.forEach((id, entry) -> result.put(id, new CurrencyDefinition(id, entry.display(), entry.symbol(),
                entry.decimalPlaces(), entry.startingBalanceMinor(), entry.maximumBalanceMinor())));
        return Map.copyOf(result);
    }

    public record CurrencyEntry(String display, String symbol, int decimalPlaces,
                                long startingBalanceMinor, long maximumBalanceMinor) { }
    public record Payments(boolean enabled, long requireConfirmationAboveMinor) { }
    public record Analytics(long extremeIssuanceWarningMinor) { }
}
