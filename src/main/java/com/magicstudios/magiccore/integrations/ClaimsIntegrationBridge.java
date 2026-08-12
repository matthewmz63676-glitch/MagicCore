package com.magicstudios.magiccore.integrations;

import com.magicstudios.magiccore.integrations.claims.GriefPreventionProtectionService;
import com.magicstudios.magiccore.protection.AllowAllProtectionService;
import com.magicstudios.magiccore.protection.ProtectionService;
import com.magicstudios.magiccore.protection.UnavailableProtectionService;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class ClaimsIntegrationBridge {
    private ClaimsIntegrationBridge() { }
    public static ProtectionService create(Plugin magicCore, String configuredProvider) {
        String provider = configuredProvider.toUpperCase(Locale.ROOT);
        Plugin griefPrevention = magicCore.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (provider.equals("NONE") || (provider.equals("AUTO") && griefPrevention == null)) return new AllowAllProtectionService();
        if ((provider.equals("AUTO") || provider.equals("GRIEFPREVENTION")) && griefPrevention != null)
            return new GriefPreventionProtectionService(magicCore, griefPrevention);
        if (provider.equals("GRIEFPREVENTION")) return new UnavailableProtectionService("GRIEFPREVENTION");
        return new UnavailableProtectionService("CLAIMS:" + provider);
    }
}
