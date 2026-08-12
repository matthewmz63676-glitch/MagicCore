package com.magicstudios.magiccore.placeholders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderRegistry {
    public static final String NAMESPACE = "magiccore";

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, FailureCounter> failures = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration reportInterval;
    private final String neutralOutput;

    public PlaceholderRegistry() {
        this(Clock.systemUTC(), Duration.ofMinutes(5), "");
    }

    public PlaceholderRegistry(Clock clock, Duration reportInterval, String neutralOutput) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.reportInterval = Objects.requireNonNull(reportInterval, "reportInterval");
        this.neutralOutput = Objects.requireNonNull(neutralOutput, "neutralOutput");
    }

    public void register(String owner, String key, PlaceholderResolver resolver) {
        String normalized = normalize(key);
        Entry existing = entries.putIfAbsent(normalized, new Entry(owner, resolver));
        if (existing != null) {
            throw new IllegalStateException("Placeholder %" + NAMESPACE + "_" + normalized + "% is already owned by " + existing.owner());
        }
    }

    public String resolve(String rawKey, PlaceholderContext context) {
        String key = normalize(rawKey);
        Entry entry = entries.get(key);
        if (entry == null) {
            return neutralOutput;
        }
        try {
            return Objects.requireNonNullElse(entry.resolver().resolve(context), neutralOutput);
        } catch (Exception failure) {
            failures.compute(key, (ignored, counter) -> (counter == null ? FailureCounter.empty() : counter)
                    .record(clock.instant(), reportInterval, failure));
            return neutralOutput;
        }
    }

    public void unregisterOwner(String owner) {
        entries.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
    }

    public Map<String, String> owners() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        entries.forEach((key, entry) -> snapshot.put(NAMESPACE + "_" + key, entry.owner()));
        return Map.copyOf(snapshot);
    }

    public Map<String, FailureSnapshot> failureSnapshot() {
        Map<String, FailureSnapshot> snapshot = new LinkedHashMap<>();
        failures.forEach((key, counter) -> snapshot.put(key,
                new FailureSnapshot(counter.total(), counter.suppressed(), counter.lastFailure(), counter.lastMessage())));
        return Map.copyOf(snapshot);
    }

    private static String normalize(String key) {
        String normalized = Objects.requireNonNull(key, "key").toLowerCase(Locale.ROOT);
        if (normalized.startsWith(NAMESPACE + "_")) {
            normalized = normalized.substring(NAMESPACE.length() + 1);
        }
        if (!normalized.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Placeholder key must be lower snake case: " + key);
        }
        return normalized;
    }

    public record FailureSnapshot(long total, long suppressed, Instant lastFailure, String lastMessage) {
    }

    private record Entry(String owner, PlaceholderResolver resolver) {
        private Entry {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(resolver, "resolver");
        }
    }

    private record FailureCounter(long total, long suppressed, Instant lastFailure,
                                  Instant nextReportAt, String lastMessage) {
        private static FailureCounter empty() {
            return new FailureCounter(0, 0, Instant.EPOCH, Instant.EPOCH, "");
        }

        private FailureCounter record(Instant now, Duration interval, Exception failure) {
            boolean reportWindow = !now.isBefore(nextReportAt);
            return new FailureCounter(total + 1, reportWindow ? suppressed : suppressed + 1, now,
                    reportWindow ? now.plus(interval) : nextReportAt,
                    failure.getClass().getSimpleName() + ": " + Objects.toString(failure.getMessage(), ""));
        }
    }
}
