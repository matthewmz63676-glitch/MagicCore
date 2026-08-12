package com.magicstudios.magiccore.modules.billford;
import java.time.Instant;
public record BillfordStock(String recipeId,int remaining,Instant updatedAt){}
