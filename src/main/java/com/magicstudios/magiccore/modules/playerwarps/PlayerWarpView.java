package com.magicstudios.magiccore.modules.playerwarps;

import java.time.Instant;

public record PlayerWarpView(PlayerWarp warp,boolean favorite,boolean promoted,Instant promotionEndsAt) { }
