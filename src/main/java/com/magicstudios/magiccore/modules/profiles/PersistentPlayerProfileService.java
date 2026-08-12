package com.magicstudios.magiccore.modules.profiles;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentPlayerProfileService implements PlayerProfileService {
    private static final int MAX_NAME_HISTORY = 10;
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final RecordRepository<PlayerProfile> profiles = new RecordRepository<>("profiles.player", PlayerProfile.class);

    public PersistentPlayerProfileService(TransactionalDataStore store, DomainEventBus events) {
        this.store = store;
        this.events = events;
    }

    @Override
    public CompletionStage<PlayerProfile> recordSeen(UUID playerId, String currentName, String locale, Instant seenAt) {
        validateName(currentName);
        return store.transact("profile-seen:" + playerId, transaction -> {
            var existing = profiles.get(transaction, playerId.toString());
            PlayerProfile updated;
            long revision;
            if (existing.isEmpty()) {
                updated = new PlayerProfile(playerId, List.of(currentName), normalizeLocale(locale), seenAt, seenAt, Map.of());
                revision = 0;
            } else {
                PlayerProfile current = existing.get().value();
                List<String> names = new ArrayList<>(current.knownNames());
                if (!current.currentName().equals(currentName)) {
                    names.removeIf(name -> name.equalsIgnoreCase(currentName));
                    names.add(currentName);
                    while (names.size() > MAX_NAME_HISTORY) names.remove(0);
                }
                updated = new PlayerProfile(playerId, names, normalizeLocale(locale), current.firstSeen(), seenAt, current.settings());
                revision = existing.get().revision();
            }
            profiles.put(transaction, playerId.toString(), updated, revision);
            return updated;
        }).thenApply(profile -> {
            events.publish(new ProfileChanged(playerId, "seen", seenAt));
            return profile;
        });
    }

    @Override
    public CompletionStage<Optional<PlayerProfile>> find(UUID playerId) {
        return store.read(reader -> profiles.get(reader, playerId.toString()).map(RecordRepository.VersionedValue::value));
    }

    @Override public CompletionStage<Optional<PlayerProfile>> findByCurrentName(String currentName) {
        validateName(currentName);
        return store.read(reader -> {
            String after = null;
            while (true) {
                var page = profiles.scanPage(reader, after, 250);
                var match = page.stream().map(RecordRepository.KeyedVersionedValue::value)
                        .filter(value -> value.currentName().equalsIgnoreCase(currentName)).findFirst();
                if (match.isPresent() || page.size() < 250) return match;
                after = page.getLast().key();
            }
        });
    }

    @Override
    public CompletionStage<PlayerProfile> setLocale(UUID playerId, String locale, String operationKey) {
        String normalized = normalizeLocale(locale);
        return mutate(playerId, operationKey, current -> new PlayerProfile(current.playerId(), current.knownNames(),
                normalized, current.firstSeen(), current.lastSeen(), current.settings()), "locale");
    }

    @Override
    public CompletionStage<PlayerProfile> setSetting(UUID playerId, String key, String value, String operationKey) {
        if (!key.matches("[a-z][a-z0-9_.-]{0,63}")) {
            throw new IllegalArgumentException("Setting key must be a stable lower-case identifier");
        }
        return mutate(playerId, operationKey, current -> {
            Map<String, String> settings = new LinkedHashMap<>(current.settings());
            settings.put(key, value);
            return new PlayerProfile(current.playerId(), current.knownNames(), current.locale(),
                    current.firstSeen(), current.lastSeen(), settings);
        }, "setting:" + key);
    }

    private CompletionStage<PlayerProfile> mutate(UUID playerId, String operationKey,
                                                   java.util.function.UnaryOperator<PlayerProfile> mutation,
                                                   String field) {
        Instant now = Instant.now();
        return store.transact("profile:" + operationKey, transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "profile", operationKey)) {
                return profiles.get(transaction, playerId.toString()).orElseThrow().value();
            }
            var existing = profiles.get(transaction, playerId.toString())
                    .orElseThrow(() -> new IllegalStateException("Profile does not exist: " + playerId));
            PlayerProfile updated = mutation.apply(existing.value());
            profiles.put(transaction, playerId.toString(), updated, existing.revision());
            return updated;
        }).thenApply(profile -> {
            events.publish(new ProfileChanged(playerId, field, now));
            return profile;
        });
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || !locale.matches("[a-zA-Z]{2,3}[_-][a-zA-Z]{2}")) {
            return "en_US";
        }
        String[] parts = locale.replace('-', '_').split("_");
        return parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
    }

    private static void validateName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Player name must contain 1..16 Minecraft-safe characters");
        }
    }
}
