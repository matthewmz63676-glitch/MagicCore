package com.magicstudios.magiccore.modules.billford;
import java.time.Instant;import java.util.UUID;
public record BillfordPlayerState(UUID playerId,String recipeId,int claims,Instant cooldownUntil,Instant updatedAt){}
