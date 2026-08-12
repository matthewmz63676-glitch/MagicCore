package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.modules.essentials.HomeService;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.modules.shop.ShopService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/** Cached synchronous view for the internal/PAPI resolver boundary. */
public final class PhaseTwoPlaceholderView {
    private final HomeService homes;
    private final PlayerSettingsService settings;
    private final PlayerWarpService warps;
    private final int productCount;
    private final Map<UUID, Snapshot> cache = new ConcurrentHashMap<>();

    public PhaseTwoPlaceholderView(HomeService homes, PlayerSettingsService settings,
                                   PlayerWarpService warps, ShopService shop) {
        this.homes = homes; this.settings = settings; this.warps = warps;
        this.productCount = shop == null ? 0 : shop.products().size();
    }

    public void register(String owner, PlaceholderRegistry registry) {
        registry.register(owner, "essentials_homes", context -> value(context, s -> Integer.toString(s.homes())));
        registry.register(owner, "settings_tpa", context -> value(context, s -> Boolean.toString(s.tpa())));
        registry.register(owner, "playerwarps_count", context -> value(context, s -> Integer.toString(s.playerWarps())));
        registry.register(owner, "shop_products", context -> Integer.toString(productCount));
    }

    public CompletionStage<Void> refresh(UUID playerId) {
        var homeFuture = homes.homes(playerId).toCompletableFuture();
        var settingFuture = settings.get(playerId).toCompletableFuture();
        var warpFuture = warps.ownedBy(playerId).toCompletableFuture();
        return CompletableFuture.allOf(homeFuture, settingFuture, warpFuture).thenRun(() -> cache.put(playerId,
                new Snapshot(homeFuture.join().size(), settingFuture.join().enabled(PlayerSetting.TELEPORT_REQUESTS),
                        warpFuture.join().size())));
    }
    public void invalidate(UUID playerId) { cache.remove(playerId); }
    private String value(PlaceholderContext context, java.util.function.Function<Snapshot, String> getter) {
        Snapshot snapshot = context.subjectId() == null ? null : cache.get(context.subjectId());
        return snapshot == null ? "" : getter.apply(snapshot);
    }
    private record Snapshot(int homes, boolean tpa, int playerWarps) { }
}
