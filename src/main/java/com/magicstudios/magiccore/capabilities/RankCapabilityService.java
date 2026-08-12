package com.magicstudios.magiccore.capabilities;

import com.magicstudios.magiccore.ranks.RankService;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class RankCapabilityService implements CapabilityService {
    private final RankService ranks;

    public RankCapabilityService(RankService ranks) {
        this.ranks = ranks;
    }

    @Override
    public CompletionStage<Boolean> has(UUID playerId, String capability) {
        String normalized = capability.toUpperCase(Locale.ROOT);
        return ranks.rankOf(playerId).thenApply(rank -> {
            var capabilities = ranks.catalog().capabilities(rank);
            return capabilities.contains("ALL") || capabilities.contains(normalized);
        });
    }

    @Override
    public CompletionStage<Integer> limit(UUID playerId, String limitId) {
        String normalized = limitId.toUpperCase(Locale.ROOT);
        return ranks.rankOf(playerId).thenApply(rank -> ranks.catalog().limit(rank, normalized));
    }

    @Override
    public CompletionStage<Boolean> canTarget(UUID actorId, UUID targetId) {
        return ranks.rankOf(actorId).thenCombine(ranks.rankOf(targetId), (actor, target) ->
                ranks.catalog().require(actor).weight() > ranks.catalog().require(target).weight());
    }
}
