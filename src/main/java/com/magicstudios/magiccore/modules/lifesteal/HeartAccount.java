package com.magicstudios.magiccore.modules.lifesteal;
import java.time.Instant;
import java.util.UUID;
public record HeartAccount(UUID playerId,int hearts,boolean eliminated,Instant updatedAt){}
