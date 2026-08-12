package com.magicstudios.magiccore.ranks;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class InternalRankService implements RankService {
    private final TransactionalDataStore store;
    private final DomainEventBus events;
    private final RankCatalog catalog;
    private final Clock clock;
    private final RecordRepository<RankMembership> memberships =
            new RecordRepository<>("ranks.membership", RankMembership.class);

    public InternalRankService(TransactionalDataStore store, DomainEventBus events, RankCatalog catalog, Clock clock) {
        this.store = store;
        this.events = events;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    public RankCatalog catalog() {
        return catalog;
    }

    @Override
    public CompletionStage<String> rankOf(UUID playerId) {
        return store.read(reader -> memberships.get(reader, playerId.toString())
                .map(value -> value.value().rankId()).orElse(catalog.defaultRank()));
    }

    @Override
    public CompletionStage<RankChange> setRank(UUID playerId, String rankId, String actor, String operationKey) {
        catalog.require(rankId);
        return store.transact("rank-change:" + operationKey, transaction -> {
            var existing = memberships.get(transaction, playerId.toString());
            String previous = existing.map(value -> value.value().rankId()).orElse(catalog.defaultRank());
            if (!IdempotencyKeys.reserve(transaction, "rank-change", operationKey)) {
                return new RankChange(false, previous, previous);
            }
            RankMembership updated = new RankMembership(playerId, rankId, clock.instant(), actor);
            memberships.put(transaction, playerId.toString(), updated,
                    existing.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return new RankChange(true, previous, rankId);
        }).thenApply(change -> {
            if (change.applied()) events.publish(new RankChanged(playerId, change.previousRank(), change.currentRank(), actor, clock.instant()));
            return change;
        });
    }

    @Override
    public CompletionStage<RankSyncPreview> previewSync(UUID playerId, String rankId) {
        catalog.require(rankId);
        return rankOf(playerId).thenApply(current -> new RankSyncPreview(playerId.toString(), current, rankId,
                Set.of("group." + rankId.toLowerCase()), Set.of("group." + current.toLowerCase()), Set.of()));
    }
}
