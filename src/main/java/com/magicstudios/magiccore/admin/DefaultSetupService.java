package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.api.ProviderMode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultSetupService implements SetupService {
    private final ConcurrentHashMap<UUID, SetupPlan> sessions = new ConcurrentHashMap<>();

    @Override
    public SetupPlan begin(UUID actorId, SetupPreset preset) {
        Map<String, ProviderMode> features = new LinkedHashMap<>();
        for(String id:java.util.List.of("profiles","economy","ranks","teams","rewards","essentials","shop","auctions","orders","bounties","crates","display","settings","item-worth","afk-shards","presentation","menus","player-warps","store","vaults","gemshop","billford-trade","custom-tools","keyalls","koth","vote-party"))features.put(id,ProviderMode.INTERNAL);
        features.put("lifesteal",preset==SetupPreset.LIFESTEAL_SMP||preset==SetupPreset.DONUT_LIKE?ProviderMode.INTERNAL:ProviderMode.DISABLED);
        features.put("combat",preset==SetupPreset.CUSTOM?ProviderMode.DISABLED:ProviderMode.INTERNAL);
        features.put("spawn-stash",preset==SetupPreset.CUSTOM?ProviderMode.DISABLED:ProviderMode.INTERNAL);
        features.put("fast-crystal",preset==SetupPreset.DONUT_LIKE?ProviderMode.INTERNAL:ProviderMode.DISABLED);
        if(preset==SetupPreset.CUSTOM)for(String id:new java.util.ArrayList<>(features.keySet()))if(!java.util.Set.of("profiles","economy","ranks","teams","rewards").contains(id))features.put(id,ProviderMode.DISABLED);
        SetupPlan plan = new SetupPlan(preset, "SQLITE", features,
                Map.of("vault", true, "placeholderapi", true, "luckperms", false, "vulcan-evidence", true, "nuvotifier", preset!=SetupPreset.CUSTOM), false);
        sessions.put(actorId, plan);
        return plan;
    }

    @Override
    public SetupPlan selectStorage(UUID actorId, String provider) {
        String normalized = provider.toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("SQLITE", "MARIADB", "MONGODB").contains(normalized)) {
            throw new IllegalArgumentException("Storage provider must be SQLITE, MARIADB, or MONGODB");
        }
        return sessions.compute(actorId, (ignored, current) -> {
            if (current == null) throw new IllegalStateException("No setup session is active");
            return new SetupPlan(current.preset(), normalized, current.featureProviders(), current.integrations(), false);
        });
    }

    @Override
    public SetupPlan review(UUID actorId) {
        return sessions.compute(actorId, (ignored, current) -> {
            if (current == null) throw new IllegalStateException("No setup session is active");
            return current.markReviewed();
        });
    }

    @Override
    public Optional<SetupPlan> active(UUID actorId) {
        return Optional.ofNullable(sessions.get(actorId));
    }

    @Override
    public boolean cancel(UUID actorId) {
        return sessions.remove(actorId) != null;
    }
}
