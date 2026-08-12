package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;
import com.magicstudios.magiccore.api.ProviderBinding;
import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.config.ConfigurationWarnings;
import com.magicstudios.magiccore.config.MagicCoreConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BugRegressionTest {
    @TempDir Path directory;

    @Test
    void noPublicVoteKitOrBuyAliasIsRegistered() throws Exception {
        String pluginYaml = Files.readString(Path.of("src/main/resources/plugin.yml"));
        assertThat(pluginYaml).doesNotContain("votekit", "  buy:", "aliases: [buy]");
        assertThat(pluginYaml).contains("magic:");
    }

    @Test
    void sourceContainsNoConsoleCommandGlueOrRawProviderCommandConstruction() throws Exception {
        List<Path> violations;
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            violations = paths.filter(path -> path.toString().endsWith(".java")).filter(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("dispatchCommand(")
                            || source.contains("consoleSender") || source.contains("getConsoleSender(");
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
            }).toList();
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void externalProviderHealthNeverSilentlyChangesOwnership() {
        Runnable selected = () -> { };
        ProviderBinding<Runnable> binding = new ProviderBinding<>(ProviderMode.EXTERNAL, "Vault:ExampleEconomy", selected,
                HealthReport.healthy("economy-provider"));
        binding.updateHealth(new HealthReport("economy-provider", HealthState.DEGRADED, "plugin disappeared",
                Map.of(), Instant.now()));

        assertThat(binding.mode()).isEqualTo(ProviderMode.EXTERNAL);
        assertThat(binding.providerId()).isEqualTo("Vault:ExampleEconomy");
        assertThat(binding.service()).isSameAs(selected);
        assertThat(binding.health().state()).isEqualTo(HealthState.DEGRADED);
    }

    @Test
    void implementedThroughPhaseSevenAreEnabledAndGuiLayoutsUseTheSharedSecureFramework() throws Exception {
        assertThat(Path.of("src/main/java/com/magicstudios/magiccore/gui/MagicGuiController.java")).exists();
        assertThat(Path.of("src/main/resources/modules/menus.yml")).exists();
        assertThat(Path.of("src/main/resources/modules/events.yml")).exists();
        String features = Files.readString(Path.of("src/main/resources/features.yml"));
        assertThat(features).contains("shop: INTERNAL", "auctions: INTERNAL", "orders: INTERNAL", "bounties: INTERNAL",
                "lifesteal: INTERNAL", "combat: INTERNAL", "crates: INTERNAL", "display: INTERNAL", "store: INTERNAL",
                "menus: INTERNAL", "koth: INTERNAL", "vote-party: INTERNAL");
    }

    @Test
    void defaultRewardsStayBelowExtremeIssuanceWarning() throws Exception {
        var config = new MagicCoreConfigurationLoader(directory,
                relative -> getClass().getClassLoader().getResourceAsStream(relative)).installAndLoad();
        assertThat(ConfigurationWarnings.collect(config)).isEmpty();
    }
}
