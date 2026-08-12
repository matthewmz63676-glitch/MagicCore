package com.magicstudios.magiccore.modules.billford;
import java.time.Instant;import java.util.List;
public record BillfordPreview(BillfordRecipe recipe,int remainingStock,int playerClaims,Instant cooldownUntil,boolean eligible,String code,List<BillfordReward>possibleRewards){public BillfordPreview{possibleRewards=List.copyOf(possibleRewards);}}
