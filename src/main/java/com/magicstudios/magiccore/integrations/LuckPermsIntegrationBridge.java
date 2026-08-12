package com.magicstudios.magiccore.integrations;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.integrations.luckperms.LuckPermsCapabilityService;
import com.magicstudios.magiccore.integrations.luckperms.LuckPermsRankService;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankService;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.Plugin;

public final class LuckPermsIntegrationBridge {
    private LuckPermsIntegrationBridge() {
    }

    public static RankService rankService(Plugin plugin, RankCatalog catalog) {
        LuckPerms api = requireApi(plugin);
        return new LuckPermsRankService(api, catalog);
    }

    public static CapabilityService capabilityService(Plugin plugin, RankService ranks, boolean hybrid) {
        return new LuckPermsCapabilityService(requireApi(plugin), ranks, hybrid);
    }

    private static LuckPerms requireApi(Plugin plugin) {
        LuckPerms api = plugin.getServer().getServicesManager().load(LuckPerms.class);
        if (api == null) throw new IllegalStateException("LuckPerms plugin is present but its API service is unavailable");
        return api;
    }
}
