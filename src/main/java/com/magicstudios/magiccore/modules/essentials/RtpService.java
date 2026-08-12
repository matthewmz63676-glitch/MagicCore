package com.magicstudios.magiccore.modules.essentials;

import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletionStage;

public interface RtpService {
    CompletionStage<RtpResult> randomTeleport(Player player, World world, RtpBounds bounds, String operationKey);
}
