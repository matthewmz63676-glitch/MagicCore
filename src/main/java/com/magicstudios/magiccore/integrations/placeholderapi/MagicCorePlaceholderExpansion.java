package com.magicstudios.magiccore.integrations.placeholderapi;

import com.magicstudios.magiccore.placeholders.PlaceholderContext;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class MagicCorePlaceholderExpansion extends PlaceholderExpansion {
    private final Plugin plugin;
    private final PlaceholderRegistry placeholders;

    public MagicCorePlaceholderExpansion(Plugin plugin, PlaceholderRegistry placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    @Override
    public @NotNull String getIdentifier() {
        return PlaceholderRegistry.NAMESPACE;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        UUID subject = player == null ? null : player.getUniqueId();
        return placeholders.resolve(params, new PlaceholderContext(subject, subject));
    }
}
