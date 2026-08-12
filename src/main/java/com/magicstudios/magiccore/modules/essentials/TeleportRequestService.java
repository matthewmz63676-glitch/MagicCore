package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestService {
    private final ConcurrentHashMap<UUID, TeleportRequest> byTarget = new ConcurrentHashMap<>();
    private final PlayerSettingsService settings;
    private final Clock clock;
    private final Duration lifetime;

    public TeleportRequestService(PlayerSettingsService settings, Clock clock, Duration lifetime) {
        this.settings = settings;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    public CompletionStage<TeleportRequest> request(UUID requester, UUID target, TeleportRequest.Direction direction) {
        if (requester.equals(target)) throw new IllegalArgumentException("Cannot request teleport to yourself");
        return settings.get(target).thenApply(preferences -> {
            if (!preferences.enabled(PlayerSetting.TELEPORT_REQUESTS)) throw new IllegalStateException("REQUESTS_DISABLED");
            var now = clock.instant();
            TeleportRequest request = new TeleportRequest(requester, target, direction, now, now.plus(lifetime));
            byTarget.put(target, request);
            return request;
        });
    }

    public Optional<TeleportRequest> accept(UUID target) {
        TeleportRequest request = byTarget.remove(target);
        if (request == null || !request.expiresAt().isAfter(clock.instant())) return Optional.empty();
        return Optional.of(request);
    }

    public boolean deny(UUID target) {
        return byTarget.remove(target) != null;
    }

    public boolean cancel(UUID requester) {
        return byTarget.entrySet().removeIf(entry -> entry.getValue().requesterId().equals(requester));
    }

    public int purgeExpired() {
        int before = byTarget.size();
        byTarget.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(clock.instant()));
        return before - byTarget.size();
    }
}
