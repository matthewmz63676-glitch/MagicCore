package com.magicstudios.magiccore.integrations;

import com.magicstudios.magiccore.integrations.placeholderapi.MagicCorePlaceholderExpansion;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import org.bukkit.plugin.Plugin;

public final class PlaceholderApiIntegrationBridge {
    private PlaceholderApiIntegrationBridge() {
    }

    public static AutoCloseable register(Plugin plugin, PlaceholderRegistry placeholders) {
        MagicCorePlaceholderExpansion expansion = new MagicCorePlaceholderExpansion(plugin, placeholders);
        expansion.register();
        return expansion::unregister;
    }
}
