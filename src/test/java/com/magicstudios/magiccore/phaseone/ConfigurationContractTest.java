package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.config.MagicCoreConfiguration;
import com.magicstudios.magiccore.config.MagicCoreConfigurationLoader;
import com.magicstudios.magiccore.config.YamlConfigCodec;
import com.magicstudios.magiccore.config.model.RanksFile;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationContractTest {
    @TempDir
    Path directory;

    @Test
    void bundledConfigsLoadAsOneValidatedUxContract() throws Exception {
        MagicCoreConfiguration config = load();

        assertThat(config.features().features()).containsEntry("profiles", ProviderMode.INTERNAL)
                .containsEntry("economy", ProviderMode.INTERNAL)
                .containsEntry("lifesteal", ProviderMode.INTERNAL)
                .containsEntry("combat", ProviderMode.INTERNAL);
        assertThat(config.storage().provider()).isEqualTo("SQLITE");
        assertThat(config.core().serverName()).isEqualTo("MagicCore SMP");
        assertThat(config.economy().primaryCurrency()).isEqualTo("COINS");
        assertThat(config.teams().namePolicy().maximumLength()).isEqualTo(16);
        assertThat(config.integrations().placeholderapi().enabled()).isTrue();
        assertThat(config.lifesteal().startingHearts()).isEqualTo(10);
        assertThat(config.lifesteal().revivalItem().material()).isEqualTo("TOTEM_OF_UNDYING");
        assertThat(config.combat().tagDurationSeconds()).isEqualTo(15);
    }

    @Test
    void shipsExactlyFiveDonorAndFiveStaffExamplesWithStableIds() throws Exception {
        MagicCoreConfiguration config = load();
        assertThat(config.ranks().definitions().values()).filteredOn(rank -> rank.type() == RankType.DONOR).hasSize(5);
        assertThat(config.ranks().definitions().values()).filteredOn(rank -> rank.type() == RankType.STAFF).hasSize(5);
        assertThat(config.ranks().definitions()).containsKeys("VIP", "VIP_PLUS", "MVP", "ELITE", "LEGEND",
                "HELPER", "MODERATOR", "SENIOR_MODERATOR", "ADMIN", "OWNER");
        new RankCatalog(config.ranks().system().defaultRank(), config.ranks().definitions());
    }

    @Test
    void ranksConfigurationRoundTripsWithoutLosingTypedMeaning() throws Exception {
        MagicCoreConfiguration config = load();
        YamlConfigCodec<RanksFile> codec = new YamlConfigCodec<>(RanksFile.class);
        RanksFile roundTripped = codec.decode(codec.encode(config.ranks()));
        assertThat(roundTripped).isEqualTo(config.ranks());
        assertThat(new String(codec.encode(config.ranks()))).contains("config-version:", "default-rank:", "VIP_PLUS:");
    }

    private MagicCoreConfiguration load() throws Exception {
        return new MagicCoreConfigurationLoader(directory,
                relative -> getClass().getClassLoader().getResourceAsStream(relative)).installAndLoad();
    }
}
