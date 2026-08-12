package com.magicstudios.magiccore.integrations;

import com.magicstudios.magiccore.protection.ProtectionService;
import org.bukkit.plugin.Plugin;

/** Keeps optional WorldGuard types out of core class signatures. */
public final class WorldGuardIntegrationBridge {
    private WorldGuardIntegrationBridge() { }

    public static ProtectionService create(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null)
            throw new IllegalStateException("WorldGuard is configured but unavailable");
        return new com.magicstudios.magiccore.integrations.worldguard.WorldGuardProtectionService(plugin);
    }
}
