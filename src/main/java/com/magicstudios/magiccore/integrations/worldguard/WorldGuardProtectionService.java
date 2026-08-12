package com.magicstudios.magiccore.integrations.worldguard;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionDecision;
import com.magicstudios.magiccore.protection.ProtectionService;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Loaded only after the optional plugin has been positively detected. Calls must originate in the location's region context. */
public final class WorldGuardProtectionService implements ProtectionService {
    private final Plugin plugin;
    public WorldGuardProtectionService(Plugin plugin) { this.plugin = plugin; }

    @Override
    public CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action) {
        World world = plugin.getServer().getWorld(position.worldId());
        Player player = plugin.getServer().getPlayer(playerId);
        if (world == null || player == null)
            return CompletableFuture.completedFuture(ProtectionDecision.deny("WORLDGUARD", "CONTEXT_UNAVAILABLE"));
        Location location = new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        StateFlag flag = switch (action) {
            case TELEPORT_IN -> Flags.ENTRY;
            case PVP -> Flags.PVP;
            case BLOCK_BREAK -> Flags.BLOCK_BREAK;
            case BLOCK_PLACE -> Flags.BLOCK_PLACE;
            case INTERACT, SHOP_USE, SET_HOME, CREATE_WARP -> Flags.USE;
        };
        boolean allowed = query.testState(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), flag);
        return CompletableFuture.completedFuture(allowed
                ? ProtectionDecision.allow("WORLDGUARD")
                : ProtectionDecision.deny("WORLDGUARD", "FLAG_DENIED:" + flag.getName()));
    }

    @Override public String providerId() { return "WORLDGUARD"; }
}
