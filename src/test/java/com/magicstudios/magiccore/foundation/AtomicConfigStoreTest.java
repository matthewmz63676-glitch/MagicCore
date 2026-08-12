package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.ConfigChange;
import com.magicstudios.magiccore.config.ConfigRevisionConflictException;
import com.magicstudios.magiccore.config.ConfigValidationException;
import com.magicstudios.magiccore.config.ValidationIssue;
import com.magicstudios.magiccore.config.YamlConfigCodec;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtomicConfigStoreTest {
    @TempDir
    Path directory;
    private BoundedIoExecutor executor;

    @AfterEach
    void closeExecutor() {
        if (executor != null) executor.close();
    }

    @Test
    void roundTripsBacksUpAndDetectsStaleEditorRevision() throws Exception {
        AtomicConfigStore<SampleConfig> store = store();
        store.loadOrCreate(new SampleConfig(1, "alpha")).toCompletableFuture().join();

        var committed = store.commit(new ConfigChange<>(1, "admin", "COMMAND", "rename", false,
                current -> new SampleConfig(current.configVersion(), "beta"))).toCompletableFuture().join();

        assertThat(committed.snapshot().revision()).isEqualTo(2);
        assertThat(committed.backup()).exists();
        assertThat(Files.readString(directory.resolve("config.yml"))).contains("beta");
        assertThatThrownBy(() -> store.commit(new ConfigChange<>(1, "admin", "GUI", "stale", false,
                        current -> current)).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ConfigRevisionConflictException.class);
    }

    @Test
    void invalidCandidateNeverReplacesLastValidSnapshot() {
        AtomicConfigStore<SampleConfig> store = store();
        store.loadOrCreate(new SampleConfig(1, "alpha")).toCompletableFuture().join();

        assertThatThrownBy(() -> store.commit(new ConfigChange<>(1, "admin", "YAML", "bad", false,
                        current -> new SampleConfig(1, ""))).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ConfigValidationException.class);
        assertThat(store.snapshot().value().name()).isEqualTo("alpha");
        assertThat(store.snapshot().revision()).isEqualTo(1);
    }

    private AtomicConfigStore<SampleConfig> store() {
        executor = new BoundedIoExecutor(1, 16, "config-test");
        return new AtomicConfigStore<>(directory.resolve("config.yml"), directory.resolve("backups"),
                new YamlConfigCodec<>(SampleConfig.class), candidate -> candidate.name().isBlank()
                ? List.of(ValidationIssue.error("config.yml:name", "must not be blank", "set a name")) : List.of(), executor);
    }

    record SampleConfig(int configVersion, String name) {
    }
}
