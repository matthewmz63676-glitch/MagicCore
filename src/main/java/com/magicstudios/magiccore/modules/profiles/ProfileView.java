package com.magicstudios.magiccore.modules.profiles;

import com.magicstudios.magiccore.modules.settings.PlayerSetting;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Authorization-complete profile model safe for command or GUI rendering. */
public record ProfileView(UUID playerId, boolean visible, boolean administrative,
                          String denialReason, String currentName, String rankId,
                          Instant firstSeen, Instant lastSeen, long kills, long deaths,
                          long playtimeSeconds, long shards,
                          Map<PlayerSetting, Boolean> settings,
                          List<UUID> auditEventIds, List<UUID> economyTransactionIds) {
    public ProfileView {
        settings = Map.copyOf(settings);
        auditEventIds = List.copyOf(auditEventIds);
        economyTransactionIds = List.copyOf(economyTransactionIds);
    }

    public static ProfileView denied(UUID playerId) {
        return new ProfileView(playerId, false, false, "PROFILE_PRIVATE", "", "", Instant.EPOCH,
                Instant.EPOCH, 0, 0, 0, 0, Map.of(), List.of(), List.of());
    }
}
