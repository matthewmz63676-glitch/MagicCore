package com.magicstudios.magiccore.diagnostics;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DiagnosticsRegistry {
    private final Map<String, Entry> sources = new LinkedHashMap<>();

    public synchronized void register(String owner, String id, DiagnosticSource source) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        if (sources.putIfAbsent(id, new Entry(owner, source)) != null) {
            throw new IllegalStateException("Diagnostic source already registered: " + id);
        }
    }

    public synchronized Map<String, HealthReport> inspectAll() {
        Map<String, HealthReport> reports = new LinkedHashMap<>();
        sources.forEach((id, entry) -> {
            try {
                reports.put(id, entry.source().inspect());
            } catch (RuntimeException failure) {
                reports.put(id, new HealthReport(id, HealthState.FAILED,
                        failure.getClass().getSimpleName() + ": " + Objects.toString(failure.getMessage(), ""),
                        Map.of(), Instant.now()));
            }
        });
        return Map.copyOf(reports);
    }

    public synchronized void unregisterOwner(String owner) {
        sources.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
    }

    private record Entry(String owner, DiagnosticSource source) {
        private Entry {
            Objects.requireNonNull(source, "source");
        }
    }
}
