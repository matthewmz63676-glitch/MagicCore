package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.modules.combat.FastCrystalPolicy;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class FastCrystalPolicyTest {
    @Test void validatesToggleWorldRangeSightAndCooldown() {
        UUID player = UUID.randomUUID(); Instant now = Instant.parse("2026-08-11T00:00:00Z");
        var policy = new FastCrystalPolicy(true, Duration.ofMillis(250), 6, true, Set.of("world_pvp"));
        assertThat(policy.evaluate(player, false, "world_pvp", 1, true, now)).isEqualTo(FastCrystalPolicy.Decision.PLAYER_DISABLED);
        assertThat(policy.evaluate(player, true, "world", 1, true, now)).isEqualTo(FastCrystalPolicy.Decision.WORLD_BLOCKED);
        assertThat(policy.evaluate(player, true, "world_pvp", 37, true, now)).isEqualTo(FastCrystalPolicy.Decision.RANGE);
        assertThat(policy.evaluate(player, true, "world_pvp", 4, false, now)).isEqualTo(FastCrystalPolicy.Decision.LINE_OF_SIGHT);
        assertThat(policy.evaluate(player, true, "world_pvp", 4, true, now)).isEqualTo(FastCrystalPolicy.Decision.ALLOWED);
        assertThat(policy.evaluate(player, true, "world_pvp", 4, true, now.plusMillis(1))).isEqualTo(FastCrystalPolicy.Decision.COOLDOWN);
        assertThat(policy.evaluate(player, true, "world_pvp", 4, true, now.plusMillis(251))).isEqualTo(FastCrystalPolicy.Decision.ALLOWED);
    }
}
