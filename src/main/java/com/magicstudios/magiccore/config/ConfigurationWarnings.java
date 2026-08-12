package com.magicstudios.magiccore.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigurationWarnings {
    private ConfigurationWarnings() {
    }

    public static List<String> collect(MagicCoreConfiguration config) {
        List<String> warnings = new ArrayList<>();
        long threshold = config.economy().analytics().extremeIssuanceWarningMinor();
        config.rewards().daily().definitions().stream()
                .filter(reward -> reward.amountMinor() >= threshold)
                .forEach(reward -> warnings.add("modules/rewards.yml daily reward " + reward.id()
                        + " issues " + reward.amountMinor() + " minor units, at or above the configured extreme issuance threshold"));
        config.rewards().playtime().definitions().stream()
                .filter(reward -> reward.amountMinor() >= threshold)
                .forEach(reward -> warnings.add("modules/rewards.yml playtime reward " + reward.id()
                        + " issues " + reward.amountMinor() + " minor units, at or above the configured extreme issuance threshold"));
        return List.copyOf(warnings);
    }
}
