package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentBackService implements BackService {
    private final TransactionalDataStore store;
    private final Clock clock;
    private final RecordRepository<BackLocations> locations =
            new RecordRepository<>("essentials.back", BackLocations.class);

    public PersistentBackService(TransactionalDataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public CompletionStage<Void> recordTeleportOrigin(UUID playerId, WorldPosition origin, String operationKey) {
        return update(playerId, origin, null, operationKey);
    }

    @Override
    public CompletionStage<Void> recordDeath(UUID playerId, WorldPosition deathLocation, String operationKey) {
        return update(playerId, null, deathLocation, operationKey);
    }

    @Override
    public CompletionStage<Optional<WorldPosition>> previousTeleport(UUID playerId) {
        return store.read(reader -> locations.get(reader, playerId.toString())
                .map(RecordRepository.VersionedValue::value).map(BackLocations::previousTeleport));
    }

    @Override
    public CompletionStage<Optional<WorldPosition>> lastDeath(UUID playerId) {
        return store.read(reader -> locations.get(reader, playerId.toString())
                .map(RecordRepository.VersionedValue::value).map(BackLocations::lastDeath));
    }

    private CompletionStage<Void> update(UUID playerId, WorldPosition teleport, WorldPosition death,
                                         String operationKey) {
        return store.transact("back-location:" + operationKey, tx -> {
            if (!IdempotencyKeys.reserve(tx, "back-location", operationKey)) return null;
            var current = locations.get(tx, playerId.toString());
            BackLocations previous = current.map(RecordRepository.VersionedValue::value)
                    .orElse(new BackLocations(playerId, null, null, clock.instant()));
            BackLocations updated = new BackLocations(playerId,
                    teleport == null ? previous.previousTeleport() : teleport,
                    death == null ? previous.lastDeath() : death, clock.instant());
            locations.put(tx, playerId.toString(), updated,
                    current.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return null;
        });
    }
}
