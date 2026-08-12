package com.magicstudios.magiccore.integrations.apollo;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface LunarClientService {
    enum Delivery { APOLLO, VANILLA_FALLBACK }

    String provider();
    boolean available();
    boolean isLunarClient(UUID playerId);

    /** Sends an Apollo waypoint when possible and always supplies a vanilla chat fallback. */
    Delivery displayWaypoint(Player player, LunarWaypoint waypoint, Component vanillaFallback);
}
