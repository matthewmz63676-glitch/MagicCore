package com.magicstudios.magiccore.modules.essentials;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface TeleportService {
    CompletionStage<TeleportResult> teleport(Player player, Location destination, Duration warmup, String operationKey);
    boolean observeMovement(Player player, Location current);
    boolean cancel(Player player);
}
