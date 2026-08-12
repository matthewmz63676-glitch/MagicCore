package com.magicstudios.magiccore.integrations.claims;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionDecision;
import com.magicstudios.magiccore.protection.ProtectionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Optional adapter isolated from GriefPrevention classes. Reflection failures are explicit deny decisions. */
public final class GriefPreventionProtectionService implements ProtectionService {
    private final Plugin magicCore;
    private final Plugin griefPrevention;
    public GriefPreventionProtectionService(Plugin magicCore, Plugin griefPrevention) {
        this.magicCore = magicCore; this.griefPrevention = griefPrevention;
    }

    @Override public CompletionStage<ProtectionDecision> check(UUID playerId, WorldPosition position, ProtectionAction action) {
        try {
            Player player = magicCore.getServer().getPlayer(playerId);
            World world = magicCore.getServer().getWorld(position.worldId());
            if (player == null || world == null) return completed(false, "CONTEXT_UNAVAILABLE");
            Location location = new Location(world, position.x(), position.y(), position.z());
            Object dataStore = griefPrevention.getClass().getField("dataStore").get(griefPrevention);
            Method getClaimAt = Arrays.stream(dataStore.getClass().getMethods())
                    .filter(method -> method.getName().equals("getClaimAt") && method.getParameterCount() == 3).findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("getClaimAt"));
            Object claim = getClaimAt.invoke(dataStore, location, true, null);
            if (claim == null) return CompletableFuture.completedFuture(ProtectionDecision.allow("GRIEFPREVENTION"));
            String denial;
            if (action == ProtectionAction.BLOCK_BREAK || action == ProtectionAction.BLOCK_PLACE
                    || action == ProtectionAction.CREATE_WARP || action == ProtectionAction.SET_HOME) {
                Method allowBuild = claim.getClass().getMethod("allowBuild", Player.class, Material.class);
                denial = (String) allowBuild.invoke(claim, player, Material.AIR);
            } else {
                Method allowAccess = claim.getClass().getMethod("allowAccess", Player.class);
                denial = (String) allowAccess.invoke(claim, player);
            }
            return denial == null ? CompletableFuture.completedFuture(ProtectionDecision.allow("GRIEFPREVENTION"))
                    : completed(false, "CLAIM_DENIED");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return completed(false, "CLAIM_QUERY_FAILED:" + failure.getClass().getSimpleName());
        }
    }
    private static CompletionStage<ProtectionDecision> completed(boolean allowed, String reason) {
        return CompletableFuture.completedFuture(allowed ? ProtectionDecision.allow("GRIEFPREVENTION")
                : ProtectionDecision.deny("GRIEFPREVENTION", reason));
    }
    @Override public String providerId() { return "GRIEFPREVENTION"; }
}
