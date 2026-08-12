package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.config.model.AfkFile;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

final class AfkZoneMatcher {
    private final Plugin plugin;private final List<AfkFile.Zone>zones;
    AfkZoneMatcher(Plugin plugin,List<AfkFile.Zone>zones){this.plugin=plugin;this.zones=List.copyOf(zones);}
    Optional<String>match(Location location){for(AfkFile.Zone zone:zones){if(zone.type().equalsIgnoreCase("NATIVE")&&nativeMatch(zone,location))return Optional.of(zone.id());if(zone.type().equalsIgnoreCase("WORLDGUARD")&&worldGuardMatch(zone,location))return Optional.of(zone.id());}return Optional.empty();}
    private static boolean nativeMatch(AfkFile.Zone zone,Location location){return location.getWorld()!=null&&location.getWorld().getName().equalsIgnoreCase(zone.world())&&location.getBlockX()>=zone.minimumX()&&location.getBlockX()<=zone.maximumX()&&location.getBlockY()>=zone.minimumY()&&location.getBlockY()<=zone.maximumY()&&location.getBlockZ()>=zone.minimumZ()&&location.getBlockZ()<=zone.maximumZ();}
    private boolean worldGuardMatch(AfkFile.Zone zone,Location location){if(plugin.getServer().getPluginManager().getPlugin("WorldGuard")==null)return false;try{var query=WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();return query.getApplicableRegions(BukkitAdapter.adapt(location)).getRegions().stream().anyMatch(region->region.getId().equalsIgnoreCase(zone.worldGuardRegion()));}catch(Throwable unavailable){return false;}}
}
