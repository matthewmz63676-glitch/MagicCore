package com.magicstudios.magiccore.integrations.apollo;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Optional, reflection-isolated Apollo adapter; MagicCore never shades Apollo classes. */
public final class ApolloLunarClientService implements LunarClientService {
    public static final String JSON_CHANNEL = "apollo:json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Plugin plugin;
    private final boolean configured;
    private final Method getPlayerManager;
    private final Method getPlayer;

    private ApolloLunarClientService(Plugin plugin, boolean configured, Method getPlayerManager, Method getPlayer) {
        this.plugin = plugin;
        this.configured = configured;
        this.getPlayerManager = getPlayerManager;
        this.getPlayer = getPlayer;
    }

    public static ApolloLunarClientService create(Plugin plugin, String provider, boolean enabled) {
        if (!enabled || !"APOLLO".equalsIgnoreCase(provider)) {
            return new ApolloLunarClientService(plugin, false, null, null);
        }
        try {
            Class<?> apollo = Class.forName("com.lunarclient.apollo.Apollo");
            Method managerMethod = apollo.getMethod("getPlayerManager");
            Object manager = managerMethod.invoke(null);
            Method playerMethod = manager.getClass().getMethod("getPlayer", UUID.class);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, JSON_CHANNEL);
            return new ApolloLunarClientService(plugin, true, managerMethod, playerMethod);
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return new ApolloLunarClientService(plugin, true, null, null);
        }
    }

    @Override public String provider() { return "APOLLO"; }
    @Override public boolean available() { return configured && getPlayerManager != null && getPlayer != null; }

    @Override
    public boolean isLunarClient(UUID playerId) {
        if (!available()) return false;
        try {
            Object manager = getPlayerManager.invoke(null);
            Object result = getPlayer.invoke(manager, playerId);
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return false;
        }
    }

    @Override
    public Delivery displayWaypoint(Player player, LunarWaypoint waypoint, Component vanillaFallback) {
        if (isLunarClient(player.getUniqueId())) {
            try {
                player.sendPluginMessage(plugin, JSON_CHANNEL, waypointPayload(waypoint));
                return Delivery.APOLLO;
            } catch (Exception ignored) {
                // A malformed/unavailable client channel must never remove the vanilla route.
            }
        }
        player.sendMessage(vanillaFallback);
        return Delivery.VANILLA_FALLBACK;
    }

    static byte[] waypointPayload(LunarWaypoint waypoint) throws Exception {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("world", waypoint.world());
        location.put("x", waypoint.x());
        location.put("y", waypoint.y());
        location.put("z", waypoint.z());
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("@type", "type.googleapis.com/lunarclient.apollo.waypoint.v1.DisplayWaypointMessage");
        message.put("name", waypoint.name());
        message.put("location", location);
        message.put("color", Map.of("color", waypoint.rgb()));
        message.put("preventRemoval", waypoint.preventRemoval());
        message.put("hidden", false);
        return JSON.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
    }
}
