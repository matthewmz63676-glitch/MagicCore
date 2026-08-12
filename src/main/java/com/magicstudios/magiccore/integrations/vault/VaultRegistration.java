package com.magicstudios.magiccore.integrations.vault;

import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.BalanceChanged;
import com.magicstudios.magiccore.api.DomainEventBus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public final class VaultRegistration implements AutoCloseable {
    private final Plugin plugin;
    private final MagicCoreVaultEconomy provider;

    public VaultRegistration(Plugin plugin, EconomyService economy, DomainEventBus events) {
        this.plugin = plugin;
        this.provider = new MagicCoreVaultEconomy(economy);
        events.subscribe("vault-integration", BalanceChanged.class,
                event -> provider.cache(event.playerId(), event.afterMinor()));
        plugin.getServer().getOnlinePlayers().forEach(player -> economy.balance(player.getUniqueId(), economy.primaryCurrency())
                .thenAccept(balance -> provider.cache(player.getUniqueId(), balance.minorUnits())));
    }

    public void register() {
        plugin.getServer().getServicesManager().register(Economy.class, provider, plugin, ServicePriority.Normal);
    }

    @Override
    public void close() {
        provider.disable();
        plugin.getServer().getServicesManager().unregister(Economy.class, provider);
    }
}
