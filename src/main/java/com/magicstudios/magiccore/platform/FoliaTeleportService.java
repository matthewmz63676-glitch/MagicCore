package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.essentials.BackService;
import com.magicstudios.magiccore.modules.essentials.TeleportResult;
import com.magicstudios.magiccore.modules.essentials.TeleportService;
import com.magicstudios.magiccore.modules.essentials.TeleportWarmup;
import com.magicstudios.magiccore.modules.essentials.TeleportWarmupService;
import com.magicstudios.magiccore.modules.essentials.TeleportPolicyService;
import com.magicstudios.magiccore.modules.essentials.TeleportPermit;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.modules.combat.CombatService;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.Optional;
import java.util.function.Supplier;

/** Cross-region teleport coordinator. It never blocks or retains locks while handing work between schedulers. */
public final class FoliaTeleportService implements TeleportService {
    private final SchedulerFacade scheduler;
    private final TeleportWarmupService warmups;
    private final BackService back;
    private final ProtectionService protection;
    private final TeleportPolicyService policy;
    private final Supplier<Optional<CombatService>> combat;

    public FoliaTeleportService(SchedulerFacade scheduler, TeleportWarmupService warmups,
                                BackService back, ProtectionService protection, TeleportPolicyService policy,
                                Supplier<Optional<CombatService>> combat) {
        this.scheduler = scheduler;
        this.warmups = warmups;
        this.back = back;
        this.protection = protection;
        this.policy = policy;
        this.combat = combat;
    }

    @Override
    public CompletionStage<TeleportResult> teleport(Player player, Location destination, Duration warmup,
                                                     String operationKey) {
        Objects.requireNonNull(destination.getWorld(), "destination world");
        Location immutableDestination = destination.clone();
        CompletableFuture<TeleportResult> result = new CompletableFuture<>();
        scheduler.executeEntity(player, () -> {
            if (!player.isOnline()) { result.complete(TeleportResult.rejected("PLAYER_OFFLINE")); return; }
            if (combat.get().map(service -> service.isTagged(player.getUniqueId())).orElse(false)) {
                result.complete(TeleportResult.rejected("COMBAT_TAGGED")); return;
            }
            warmups.begin(player.getUniqueId(), BukkitWorldPositions.from(player.getLocation()),
                    BukkitWorldPositions.from(immutableDestination), warmup, operationKey);
            scheduler.executeGlobalLater(warmup, () -> validateDestination(player, immutableDestination, result));
        }, () -> result.complete(TeleportResult.rejected("ENTITY_RETIRED")));
        return result;
    }

    private void validateDestination(Player player, Location destination, CompletableFuture<TeleportResult> result) {
        scheduler.executeRegion(destination, () -> protection.check(player.getUniqueId(), BukkitWorldPositions.from(destination),
                ProtectionAction.TELEPORT_IN).whenComplete((decision, failure) -> {
            if (failure != null) { result.completeExceptionally(failure); return; }
            if (!decision.allowed()) { warmups.cancel(player.getUniqueId()); result.complete(TeleportResult.rejected(decision.reason())); return; }
            scheduler.executeEntity(player, () -> persistOriginThenTeleport(player, destination, result),
                    () -> result.complete(TeleportResult.rejected("ENTITY_RETIRED")));
        }));
    }

    private void persistOriginThenTeleport(Player player, Location destination, CompletableFuture<TeleportResult> result) {
        TeleportWarmup ready = warmups.takeReady(player.getUniqueId()).orElse(null);
        if (ready == null) { result.complete(TeleportResult.rejected("CANCELLED_OR_NOT_READY")); return; }
        WorldPosition currentOrigin = BukkitWorldPositions.from(player.getLocation());
        policy.reserve(player.getUniqueId(), ready.operationKey() + ":policy").whenComplete((permit, reserveFailure) -> {
            if (reserveFailure != null) { result.completeExceptionally(reserveFailure); return; }
            back.recordTeleportOrigin(player.getUniqueId(), currentOrigin, ready.operationKey() + ":back")
                .whenComplete((ignored, storageFailure) -> {
                    if (storageFailure != null) { refund(permit, "back-storage", storageFailure, result); return; }
                    scheduler.executeEntity(player, () -> player.teleportAsync(destination).whenComplete((success, teleportFailure) -> {
                        if (teleportFailure != null) refund(permit, "platform-failure", teleportFailure, result);
                        else if (!Boolean.TRUE.equals(success)) refundRejected(permit, "PLATFORM_REJECTED", result);
                        else policy.complete(permit).whenComplete((completed, policyFailure) -> {
                            if (policyFailure != null) result.completeExceptionally(policyFailure);
                            else result.complete(TeleportResult.success());
                        });
                    }), () -> refundRejected(permit, "ENTITY_RETIRED", result));
                });
        });
    }

    private void refund(TeleportPermit permit, String reason, Throwable failure, CompletableFuture<TeleportResult> result) {
        policy.refund(permit, reason).whenComplete((ignored, refundFailure) -> {
            if (refundFailure != null) failure.addSuppressed(refundFailure);
            result.completeExceptionally(failure);
        });
    }

    private void refundRejected(TeleportPermit permit, String reason, CompletableFuture<TeleportResult> result) {
        policy.refund(permit, reason).whenComplete((ignored, refundFailure) -> {
            if (refundFailure != null) result.completeExceptionally(refundFailure);
            else result.complete(TeleportResult.rejected(reason));
        });
    }

    @Override public boolean observeMovement(Player player, Location current) {
        return warmups.observeMovement(player.getUniqueId(), BukkitWorldPositions.from(current));
    }
    @Override public boolean cancel(Player player) { return warmups.cancel(player.getUniqueId()); }
}
