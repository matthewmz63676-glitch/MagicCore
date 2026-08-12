package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.essentials.RtpBounds;
import com.magicstudios.magiccore.modules.essentials.RtpCandidatePlanner;
import com.magicstudios.magiccore.modules.essentials.RtpResult;
import com.magicstudios.magiccore.modules.essentials.RtpService;
import com.magicstudios.magiccore.modules.essentials.TeleportService;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;

/** Each terrain read executes in its candidate region; candidates are bounded and checked sequentially. */
public final class FoliaRtpService implements RtpService {
    private static final Set<Material> UNSAFE_GROUND = Set.of(Material.LAVA, Material.WATER, Material.FIRE,
            Material.SOUL_FIRE, Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE);
    private final SchedulerFacade scheduler;
    private final TeleportService teleports;
    private final ProtectionService protection;
    private final RtpCandidatePlanner planner = new RtpCandidatePlanner();
    private final Duration warmup;

    public FoliaRtpService(SchedulerFacade scheduler, TeleportService teleports, ProtectionService protection,
                           Duration warmup) {
        this.scheduler = scheduler; this.teleports = teleports; this.protection = protection; this.warmup = warmup;
    }

    @Override
    public CompletionStage<RtpResult> randomTeleport(Player player, World world, RtpBounds bounds, String operationKey) {
        List<RtpCandidatePlanner.Candidate> candidates = planner.plan(bounds, ThreadLocalRandom.current());
        CompletableFuture<RtpResult> result = new CompletableFuture<>();
        probe(player, world, candidates, 0, operationKey, result);
        return result;
    }

    private void probe(Player player, World world, List<RtpCandidatePlanner.Candidate> candidates, int index,
                       String operationKey, CompletableFuture<RtpResult> result) {
        if (index >= candidates.size()) {
            result.complete(new RtpResult(false, "NO_SAFE_LOCATION", null, candidates.size())); return;
        }
        var candidate = candidates.get(index);
        Location regionKey = new Location(world, Math.floor(candidate.x()), world.getMinHeight(), Math.floor(candidate.z()));
        scheduler.executeRegion(regionKey, () -> {
            int x = regionKey.getBlockX(); int z = regionKey.getBlockZ();
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) { probe(player, world, candidates, index + 1, operationKey, result); return; }
            Location destination = new Location(world, x + 0.5, y, z + 0.5);
            if (!world.getWorldBorder().isInside(destination) || !safe(destination)) {
                probe(player, world, candidates, index + 1, operationKey, result); return;
            }
            WorldPosition position = BukkitWorldPositions.from(destination);
            protection.check(player.getUniqueId(), position, ProtectionAction.TELEPORT_IN).whenComplete((decision, failure) -> {
                if (failure != null) { result.completeExceptionally(failure); return; }
                if (!decision.allowed()) { probe(player, world, candidates, index + 1, operationKey, result); return; }
                teleports.teleport(player, destination, warmup, operationKey).whenComplete((teleport, teleportFailure) -> {
                    if (teleportFailure != null) result.completeExceptionally(teleportFailure);
                    else result.complete(new RtpResult(teleport.completed(), teleport.code(), position, index + 1));
                });
            });
        });
    }

    private static boolean safe(Location destination) {
        Block ground = destination.clone().add(0, -1, 0).getBlock();
        Block feet = destination.getBlock();
        Block head = destination.clone().add(0, 1, 0).getBlock();
        return ground.getType().isSolid() && !UNSAFE_GROUND.contains(ground.getType())
                && feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid();
    }
}
