package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.export.SanitizedExportPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureGuardTest {
    @Test
    void rawSchedulersExistOnlyInsidePlatformAdapter() throws Exception {
        Path sourceRoot = Path.of("src/main/java");
        List<Path> violations;
        try (var paths = Files.walk(sourceRoot)) {
            violations = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("PaperFoliaScheduler.java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("getGlobalRegionScheduler(")
                                    || source.contains("getRegionScheduler(")
                                    || source.contains("getAsyncScheduler(")
                                    || source.contains("Bukkit.getScheduler(")
                                    || source.contains(".getScheduler().");
                        } catch (Exception failure) {
                            throw new IllegalStateException(failure);
                        }
                    }).toList();
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void sanitizedExportExcludesRuntimeDataSecretsAndLibraries() {
        SanitizedExportPolicy policy = new SanitizedExportPolicy();
        assertThat(policy.include(Path.of("features.yml"))).isTrue();
        assertThat(policy.include(Path.of("data", "magiccore.db"))).isFalse();
        assertThat(policy.include(Path.of("data", "magiccore.db-wal"))).isFalse();
        assertThat(policy.include(Path.of("libraries", "driver.jar"))).isFalse();
        assertThat(policy.include(Path.of("integrations", "discord-token.yml"))).isFalse();
    }

    @Test
    void securityEvidenceIntegrationIsVulcanOnly() throws Exception {
        List<Path> roots = List.of(Path.of("src/main/java"), Path.of("src/main/resources"));
        List<Path> violations = new java.util.ArrayList<>();
        for (Path root : roots) try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(java.util.Locale.ROOT);
                    if (source.contains("anti" + "cheat") || source.contains("anti" + "-" + "cheat")
                            || source.contains("exe" + "mption") || source.contains("hidden " + "by" + "pass")) violations.add(path);
                } catch (Exception failure) { throw new IllegalStateException(failure); }
            });
        }
        assertThat(violations).isEmpty();
        assertThat(Path.of("src/main/java/com/magicstudios/magiccore/integrations/vulcan/VulcanIntegrationService.java")).exists();
    }
}
