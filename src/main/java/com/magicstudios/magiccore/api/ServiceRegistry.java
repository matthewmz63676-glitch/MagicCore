package com.magicstudios.magiccore.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {
    private final Map<Class<?>, Entry<?>> services = new ConcurrentHashMap<>();

    public <T> void register(String owner, Class<T> contract, T implementation) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(implementation, "implementation");
        if (!contract.isInstance(implementation)) {
            throw new IllegalArgumentException(implementation.getClass().getName() + " does not implement " + contract.getName());
        }
        Entry<T> candidate = new Entry<>(owner, implementation);
        Entry<?> existing = services.putIfAbsent(contract, candidate);
        if (existing != null) {
            throw new IllegalStateException("Service " + contract.getName() + " is already owned by " + existing.owner());
        }
    }

    public <T> Optional<T> find(Class<T> contract) {
        Entry<?> entry = services.get(contract);
        return entry == null ? Optional.empty() : Optional.of(contract.cast(entry.service()));
    }

    public <T> T require(Class<T> contract) {
        return find(contract).orElseThrow(() -> new IllegalStateException("Required service is unavailable: " + contract.getName()));
    }

    public void unregisterOwner(String owner) {
        services.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
    }

    public Map<String, String> snapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        services.forEach((contract, entry) -> snapshot.put(contract.getName(), entry.owner()));
        return Map.copyOf(snapshot);
    }

    private record Entry<T>(String owner, T service) {
    }
}
