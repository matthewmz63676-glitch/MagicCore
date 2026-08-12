package com.magicstudios.magiccore.integrations;

import com.magicstudios.magiccore.integrations.vault.VaultRegistration;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import org.bukkit.plugin.Plugin;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import net.milkbowl.vault.economy.Economy;

public final class VaultIntegrationBridge {
    private VaultIntegrationBridge() {
    }

    public static AutoCloseable registerInternal(Plugin plugin, EconomyService economy,
                                                 com.magicstudios.magiccore.api.DomainEventBus events) {
        VaultRegistration registration = new VaultRegistration(plugin, economy, events);
        registration.register();
        return registration;
    }

    public static boolean hasExternalProvider(Plugin plugin) {
        var registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return registration != null && !(registration.getProvider() instanceof com.magicstudios.magiccore.integrations.vault.MagicCoreVaultEconomy);
    }

    public static EconomyService consumeExternal(Plugin plugin, CurrencyDefinition primaryCurrency,
                                                 SchedulerFacade scheduler, TransactionalDataStore store,
                                                 java.time.Clock clock) {
        var registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) throw new IllegalStateException("Vault has no registered economy provider");
        return new com.magicstudios.magiccore.integrations.vault.ExternalVaultEconomyService(
                registration.getProvider(), primaryCurrency, scheduler, store, clock);
    }
}
