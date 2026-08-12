package com.magicstudios.magiccore.phasefour;

import com.magicstudios.magiccore.modules.combat.AbilityCooldownService;
import com.magicstudios.magiccore.modules.combat.NativeCombatService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CombatServiceTest {
    @Test void tagsBothPlayersExpiresAndCreditsTheOpponentOnLogout() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
        NativeCombatService combat = new NativeCombatService(clock, Duration.ofSeconds(15));
        UUID attacker = UUID.randomUUID();
        UUID victim = UUID.randomUUID();

        var tag = combat.tag(attacker, victim);
        assertThat(combat.isTagged(attacker)).isTrue();
        assertThat(combat.isTagged(victim)).isTrue();
        assertThat(combat.remaining(victim)).isEqualTo(Duration.ofSeconds(15));

        var logout = combat.logout(victim);
        assertThat(logout.resolved()).isTrue();
        assertThat(logout.kill().killerId()).isEqualTo(attacker);
        assertThat(logout.kill().victimId()).isEqualTo(victim);
        assertThat(logout.kill().verifier()).isEqualTo("COMBAT_LOGOUT");
        assertThat(combat.isTagged(attacker)).isFalse();
        assertThat(combat.isTagged(victim)).isFalse();

        combat.tag(attacker, victim);
        clock.advance(Duration.ofSeconds(15));
        assertThat(combat.isTagged(attacker)).isFalse();
        assertThat(combat.purgeExpired()).isEqualTo(1);
        assertThat(tag.playerId()).isEqualTo(victim);
    }

    @Test void abilityCooldownRejectsRepeatedUseUntilExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-10T00:00:00Z"));
        AbilityCooldownService cooldowns = new AbilityCooldownService(clock);
        UUID player = UUID.randomUUID();

        assertThat(cooldowns.tryUse(player, AbilityCooldownService.Ability.ENDER_PEARL, Duration.ofSeconds(3))).isTrue();
        assertThat(cooldowns.tryUse(player, AbilityCooldownService.Ability.ENDER_PEARL, Duration.ofSeconds(3))).isFalse();
        assertThat(cooldowns.remaining(player, AbilityCooldownService.Ability.ENDER_PEARL)).isEqualTo(Duration.ofSeconds(3));
        clock.advance(Duration.ofSeconds(3));
        assertThat(cooldowns.tryUse(player, AbilityCooldownService.Ability.ENDER_PEARL, Duration.ofSeconds(3))).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
