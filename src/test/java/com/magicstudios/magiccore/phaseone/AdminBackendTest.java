package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.admin.AdminActor;
import com.magicstudios.magiccore.admin.AdminConfigBackend;
import com.magicstudios.magiccore.admin.AdminMutationRequest;
import com.magicstudios.magiccore.audit.PersistentAuditService;
import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.ConfigChange;
import com.magicstudios.magiccore.config.YamlConfigCodec;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBackendTest {
    @TempDir Path directory;

    @Test
    void capabilityIsRecheckedAtCommitTimeAndSuccessfulCommitIsAudited() {
        var configExecutor = new BoundedIoExecutor(1, 16, "admin-config-test");
        var dataStore = new InMemoryTransactionalDataStore(new BoundedIoExecutor(1, 16, "admin-audit-test"));
        try {
            AtomicConfigStore<SampleConfig> configs = new AtomicConfigStore<>(directory.resolve("config.yml"),
                    directory.resolve("backups"), new YamlConfigCodec<>(SampleConfig.class), ignored -> List.of(), configExecutor);
            configs.loadOrCreate(new SampleConfig(1, "before")).toCompletableFuture().join();
            AtomicBoolean allowed = new AtomicBoolean(true);
            var audit = new PersistentAuditService(dataStore);
            var backend = new AdminConfigBackend<>(configs,
                    (actor, capability) -> CompletableFuture.completedFuture(allowed.get()), audit, Clock.systemUTC());
            AdminActor actor = new AdminActor(UUID.randomUUID(), "Admin", false);
            AdminMutationRequest<SampleConfig> request = new AdminMutationRequest<>(actor, "MANAGE_MODULES", "config.yml",
                    "config-change-1", new ConfigChange<>(1, actor.displayName(), "COMMAND", "rename", false,
                    current -> new SampleConfig(current.configVersion(), "after")));

            assertThat(backend.preview(request).name()).isEqualTo("after");
            allowed.set(false);
            assertThatThrownBy(() -> backend.commit(request).toCompletableFuture().join())
                    .isInstanceOf(CompletionException.class).hasCauseInstanceOf(SecurityException.class);
            assertThat(configs.snapshot().revision()).isEqualTo(1);

            allowed.set(true);
            var committed = backend.commit(request).toCompletableFuture().join();
            assertThat(committed.audited()).isTrue();
            assertThat(configs.snapshot().revision()).isEqualTo(2);
            assertThat(audit.recent(null, 10).toCompletableFuture().join()).singleElement()
                    .extracting(event -> event.action()).isEqualTo("CONFIG_COMMIT");
        } finally {
            configExecutor.close();
            dataStore.close();
        }
    }

    record SampleConfig(int configVersion, String name) { }
}
