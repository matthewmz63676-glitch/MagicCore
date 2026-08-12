package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentHomeService implements HomeService {
    private final TransactionalDataStore store;
    private final CapabilityService capabilities;
    private final DomainEventBus events;
    private final Clock clock;
    private final int maximumNameLength;
    private final RecordRepository<Home> homes = new RecordRepository<>("essentials.home", Home.class);

    public PersistentHomeService(TransactionalDataStore store, CapabilityService capabilities,
                                 DomainEventBus events, Clock clock, int maximumNameLength) {
        this.store = store;
        this.capabilities = capabilities;
        this.events = events;
        this.clock = clock;
        this.maximumNameLength = maximumNameLength;
    }

    @Override
    public CompletionStage<List<Home>> homes(UUID ownerId) {
        String prefix = ownerId + ":";
        return store.read(reader -> homes.scan(reader, null, 1000).stream()
                .map(RecordRepository.VersionedValue::value)
                .filter(home -> key(home.ownerId(), home.id()).startsWith(prefix))
                .sorted(java.util.Comparator.comparing(Home::id)).toList());
    }

    @Override
    public CompletionStage<Optional<Home>> findVisible(UUID viewerId, UUID ownerId, String homeId) {
        String normalized = normalize(homeId);
        return store.read(reader -> homes.get(reader, key(ownerId, normalized))
                .map(RecordRepository.VersionedValue::value).filter(home -> home.visibleTo(viewerId)));
    }

    @Override
    public CompletionStage<HomeMutation> set(UUID ownerId, String name, WorldPosition position, String operationKey) {
        String id = normalize(name);
        return capabilities.limit(ownerId, "HOMES").thenCompose(limit -> store.transact("home-set:" + operationKey, tx -> {
            var replay = homes.get(tx, key(ownerId, id));
            if (!IdempotencyKeys.reserve(tx, "home", operationKey)) {
                return new HomeMutation(false, "REPLAY", replay.map(RecordRepository.VersionedValue::value).orElse(null));
            }
            if (replay.isEmpty()) {
                long count = homes.scan(tx, null, 1000).stream().map(RecordRepository.VersionedValue::value)
                        .filter(home -> home.ownerId().equals(ownerId)).count();
                if (count >= limit) throw new IllegalStateException("HOME_LIMIT_REACHED");
            }
            var now = clock.instant();
            Home updated = new Home(ownerId, id, name, position,
                    replay.map(value -> value.value().sharedWith()).orElse(Set.of()),
                    replay.map(value -> value.value().createdAt()).orElse(now), now);
            homes.put(tx, key(ownerId, id), updated, replay.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return new HomeMutation(true, replay.isEmpty() ? "CREATED" : "UPDATED", updated);
        })).thenApply(result -> publish(ownerId, id, result, operationKey));
    }

    @Override
    public CompletionStage<HomeMutation> delete(UUID ownerId, String name, String operationKey) {
        String id = normalize(name);
        return store.transact("home-delete:" + operationKey, tx -> {
            var current = homes.get(tx, key(ownerId, id));
            if (!IdempotencyKeys.reserve(tx, "home", operationKey)) {
                return new HomeMutation(false, "REPLAY", current.map(RecordRepository.VersionedValue::value).orElse(null));
            }
            if (current.isEmpty()) return new HomeMutation(false, "NOT_FOUND", null);
            tx.delete("essentials.home", key(ownerId, id), current.get().revision());
            return new HomeMutation(true, "DELETED", current.get().value());
        }).thenApply(result -> publish(ownerId, id, result, operationKey));
    }

    @Override
    public CompletionStage<HomeMutation> share(UUID ownerId, String name, UUID targetId, boolean shared,
                                               String operationKey) {
        String id = normalize(name);
        if (ownerId.equals(targetId)) throw new IllegalArgumentException("A home is always visible to its owner");
        return store.transact("home-share:" + operationKey, tx -> {
            var current = homes.get(tx, key(ownerId, id)).orElseThrow(() -> new IllegalStateException("HOME_NOT_FOUND"));
            if (!IdempotencyKeys.reserve(tx, "home", operationKey)) {
                return new HomeMutation(false, "REPLAY", current.value());
            }
            Set<UUID> viewers = new LinkedHashSet<>(current.value().sharedWith());
            if (shared) viewers.add(targetId); else viewers.remove(targetId);
            Home updated = new Home(ownerId, id, current.value().displayName(), current.value().position(), viewers,
                    current.value().createdAt(), clock.instant());
            homes.put(tx, key(ownerId, id), updated, current.revision());
            return new HomeMutation(true, shared ? "SHARED" : "UNSHARED", updated);
        }).thenApply(result -> publish(ownerId, id, result, operationKey));
    }

    private HomeMutation publish(UUID ownerId, String id, HomeMutation result, String operationKey) {
        if (result.applied()) events.publish(new HomeChanged(ownerId, id, result.code(), operationKey, clock.instant()));
        return result;
    }

    private String normalize(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1," + maximumNameLength + "}")) {
            throw new IllegalArgumentException("Home name must contain 1.." + maximumNameLength + " safe characters");
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static String key(UUID ownerId, String id) {
        return ownerId + ":" + id;
    }
}
