package com.magicstudios.magiccore.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ProviderBinding<T> {
    private final ProviderMode mode;
    private final String providerId;
    private final T service;
    private final AtomicReference<HealthReport> health;

    public ProviderBinding(ProviderMode mode, String providerId, T service, HealthReport initialHealth) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.service = Objects.requireNonNull(service, "service");
        this.health = new AtomicReference<>(Objects.requireNonNull(initialHealth, "initialHealth"));
    }

    public ProviderMode mode() { return mode; }
    public String providerId() { return providerId; }
    public T service() { return service; }
    public HealthReport health() { return health.get(); }

    public void updateHealth(HealthReport report) {
        health.set(Objects.requireNonNull(report, "report"));
    }
}
