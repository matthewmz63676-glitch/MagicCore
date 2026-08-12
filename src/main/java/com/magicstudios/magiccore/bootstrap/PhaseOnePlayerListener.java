package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import com.magicstudios.magiccore.placeholders.PhaseOnePlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseTwoPlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseFourPlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseFivePlaceholderView;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public final class PhaseOnePlayerListener implements Listener {
    private final PlayerProfileService profiles;
    private final PhaseOnePlaceholderView placeholders;
    private final PhaseTwoPlaceholderView phaseTwo;
    private final PhaseFourPlaceholderView phaseFour;
    private final PhaseFivePlaceholderView phaseFive;

    public PhaseOnePlayerListener(PlayerProfileService profiles, PhaseOnePlaceholderView placeholders,
                                  PhaseTwoPlaceholderView phaseTwo, PhaseFourPlaceholderView phaseFour,
                                  PhaseFivePlaceholderView phaseFive) {
        this.profiles = profiles;
        this.placeholders = placeholders;
        this.phaseTwo = phaseTwo;
        this.phaseFour = phaseFour;
        this.phaseFive = phaseFive;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        profiles.recordSeen(player.getUniqueId(), player.getName(), player.locale().toString(), Instant.now())
                .thenCompose(ignored -> CompletableFuture.allOf(placeholders.refresh(player.getUniqueId()).toCompletableFuture(),
                        phaseTwo == null ? CompletableFuture.completedFuture(null) : phaseTwo.refresh(player.getUniqueId()).toCompletableFuture(),
                        phaseFour == null ? CompletableFuture.completedFuture(null) : phaseFour.refresh(player.getUniqueId()).toCompletableFuture(),
                        phaseFive == null ? CompletableFuture.completedFuture(null) : phaseFive.refresh(player.getUniqueId()).toCompletableFuture()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        placeholders.invalidate(event.getPlayer().getUniqueId());
        if (phaseTwo != null) phaseTwo.invalidate(event.getPlayer().getUniqueId());
        if (phaseFour != null) phaseFour.invalidate(event.getPlayer().getUniqueId());
        if (phaseFive != null) phaseFive.invalidate(event.getPlayer().getUniqueId());
    }
}
