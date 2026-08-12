package com.magicstudios.magiccore.export;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class SanitizedExportPolicy {
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            "data", "cache", "logs", "libraries", "backups", "secrets", "licenses");

    public boolean include(Path relativePath) {
        for (Path part : relativePath) {
            if (EXCLUDED_DIRECTORIES.contains(part.toString().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        String name = relativePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return !(name.endsWith(".db") || name.endsWith(".db-wal") || name.endsWith(".db-shm")
                || name.endsWith(".log") || name.endsWith(".jar") || name.endsWith(".lic")
                || name.contains("secret") || name.contains("token") || name.contains("webhook"));
    }
}
