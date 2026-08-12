package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.RankCapabilityService;
import com.magicstudios.magiccore.config.MagicCoreConfigurationLoader;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.ranks.InternalRankService;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankDefinition;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankCapabilityTest {
    @TempDir Path directory;

    @Test
    void capabilitiesLimitsAndTargetScopeComeFromRankContract() throws Exception {
        var config = new MagicCoreConfigurationLoader(directory,
                relative -> getClass().getClassLoader().getResourceAsStream(relative)).installAndLoad();
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(2, 64, "rank-test"));
        try {
            RankCatalog catalog = new RankCatalog("DEFAULT", config.ranks().definitions());
            var ranks = new InternalRankService(store, new DomainEventBus(), catalog, Clock.systemUTC());
            var capabilities = new RankCapabilityService(ranks);
            UUID admin = UUID.randomUUID();
            UUID owner = UUID.randomUUID();
            UUID legend = UUID.randomUUID();
            ranks.setRank(admin, "ADMIN", "console", "rank-admin").toCompletableFuture().join();
            ranks.setRank(owner, "OWNER", "console", "rank-owner").toCompletableFuture().join();
            ranks.setRank(legend, "LEGEND", "console", "rank-legend").toCompletableFuture().join();

            assertThat(capabilities.has(admin, "BAN").toCompletableFuture().join()).isTrue();
            assertThat(capabilities.has(legend, "FLY").toCompletableFuture().join()).isTrue();
            assertThat(capabilities.limit(legend, "TEAM_SIZE").toCompletableFuture().join()).isEqualTo(15);
            assertThat(capabilities.canTarget(admin, owner).toCompletableFuture().join()).isFalse();
            assertThat(capabilities.canTarget(owner, admin).toCompletableFuture().join()).isTrue();
        } finally {
            store.close();
        }
    }

    @Test
    void inheritanceCycleIsRejectedBeforeRuntime() throws Exception {
        var config = new MagicCoreConfigurationLoader(directory,
                relative -> getClass().getClassLoader().getResourceAsStream(relative)).installAndLoad();
        Map<String, RankDefinition> definitions = new LinkedHashMap<>(config.ranks().definitions());
        RankDefinition vip = definitions.get("VIP");
        definitions.put("VIP", new RankDefinition(vip.id(), vip.type(), vip.display(), vip.weight(),
                Set.of("LEGEND"), vip.perks(), vip.abilities(), vip.limits()));

        assertThatThrownBy(() -> new RankCatalog("DEFAULT", definitions))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cycle");
    }
}
