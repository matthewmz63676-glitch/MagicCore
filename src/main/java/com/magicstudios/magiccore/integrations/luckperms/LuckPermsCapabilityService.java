package com.magicstudios.magiccore.integrations.luckperms;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.ranks.RankService;
import net.luckperms.api.LuckPerms;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class LuckPermsCapabilityService implements CapabilityService {
    private final LuckPerms luckPerms;
    private final RankService ranks;
    private final boolean hybrid;

    public LuckPermsCapabilityService(LuckPerms luckPerms, RankService ranks, boolean hybrid) {
        this.luckPerms = luckPerms;
        this.ranks = ranks;
        this.hybrid = hybrid;
    }

    @Override
    public CompletionStage<Boolean> has(UUID playerId, String capability) {
        String normalized = capability.toUpperCase(Locale.ROOT);
        return luckPerms.getUserManager().loadUser(playerId).thenApply(user -> {
            boolean raw = user.getCachedData().getPermissionData()
                    .checkPermission("magiccore.capability." + normalized.toLowerCase(Locale.ROOT)).asBoolean();
            if (!hybrid) return raw;
            String rank = ranks.catalog().definitions().containsKey(user.getPrimaryGroup().toUpperCase(Locale.ROOT))
                    ? user.getPrimaryGroup().toUpperCase(Locale.ROOT) : ranks.catalog().defaultRank();
            var capabilities = ranks.catalog().capabilities(rank);
            return raw || capabilities.contains("ALL") || capabilities.contains(normalized);
        });
    }

    @Override
    public CompletionStage<Integer> limit(UUID playerId, String limitId) {
        return ranks.rankOf(playerId).thenApply(rank -> ranks.catalog().limit(rank, limitId.toUpperCase(Locale.ROOT)));
    }

    @Override
    public CompletionStage<Boolean> canTarget(UUID actorId, UUID targetId) {
        return ranks.rankOf(actorId).thenCombine(ranks.rankOf(targetId), (actor, target) ->
                ranks.catalog().require(actor).weight() > ranks.catalog().require(target).weight());
    }
}
