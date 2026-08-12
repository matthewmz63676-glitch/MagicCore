package com.magicstudios.magiccore.diagnostics;

import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;
import org.bukkit.plugin.Plugin;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompatibilitySnapshot {
    private static final List<String>KNOWN=List.of("Vault","LuckPerms","PlaceholderAPI","WorldGuard","GriefPrevention","TAB","DiscordSRV","floodgate","Geyser-Spigot","ItemsAdder","Nexo","DecentHolograms","Citizens","RoseStacker","WildStacker","ExcellentCrates","Vulcan","Apollo-Bukkit","Votifier");
    private CompatibilitySnapshot(){}
    public static HealthReport capture(Plugin owner,Clock clock){Map<String,String>details=new LinkedHashMap<>();int installed=0;for(String id:KNOWN){Plugin plugin=owner.getServer().getPluginManager().getPlugin(id);String value=plugin==null?"absent":plugin.getPluginMeta().getVersion();details.put(id,value);if(plugin!=null)installed++;}details.put("installed-known",Integer.toString(installed));return new HealthReport("compatibility",HealthState.HEALTHY,"Detected "+installed+" known optional integration(s); explicit module/provider health remains authoritative",details,clock.instant());}
}
