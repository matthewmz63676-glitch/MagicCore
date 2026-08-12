package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.placeholders.PlaceholderContext;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceholderRegistryTest {
    @Test
    void canonicalNamespaceWorksWithoutPlaceholderApi() {
        PlaceholderRegistry registry = new PlaceholderRegistry();
        registry.register("teams", "team_name", ignored -> "Builders");

        assertThat(registry.resolve("magiccore_team_name", new PlaceholderContext(null, null))).isEqualTo("Builders");
        assertThat(registry.owners()).containsEntry("magiccore_team_name", "teams");
    }

    @Test
    void repeatedFailuresAreNeutralAndAggregated() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
        PlaceholderRegistry registry = new PlaceholderRegistry(clock, Duration.ofMinutes(5), "unavailable");
        registry.register("economy", "economy_balance", ignored -> { throw new IllegalStateException("offline"); });

        for (int i = 0; i < 10_000; i++) {
            assertThat(registry.resolve("economy_balance", new PlaceholderContext(null, null))).isEqualTo("unavailable");
        }

        assertThat(registry.failureSnapshot().get("economy_balance").total()).isEqualTo(10_000);
        assertThat(registry.failureSnapshot().get("economy_balance").suppressed()).isEqualTo(9_999);
        assertThat(registry.failureSnapshot()).hasSize(1);
    }
}
