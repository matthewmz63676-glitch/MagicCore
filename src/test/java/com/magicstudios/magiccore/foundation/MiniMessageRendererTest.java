package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.text.MiniMessageRenderer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MiniMessageRendererTest {
    @Test
    void validatesCanonicalTemplatesAtLoadTime() {
        MiniMessageRenderer renderer = new MiniMessageRenderer();
        assertThat(renderer.validateCatalog(Map.of("rewards.claimed", "<green>Reward claimed</green>")))
                .containsKey("rewards.claimed");
    }

    @Test
    void rejectsUnmatchedGradientWithFileKey() {
        MiniMessageRenderer renderer = new MiniMessageRenderer();
        assertThatThrownBy(() -> renderer.validateCatalog(Map.of("vote.received", "Thanks</gradient>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messages.yml key 'vote.received'");
    }

    @Test
    void rejectsLegacyColorGlueAsInvalidStrictMiniMessage() {
        MiniMessageRenderer renderer = new MiniMessageRenderer();
        assertThatThrownBy(() -> renderer.validate("&aLegacy text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Legacy color codes");
    }
}
