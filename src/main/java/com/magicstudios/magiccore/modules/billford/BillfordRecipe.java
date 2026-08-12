package com.magicstudios.magiccore.modules.billford;
import java.time.Duration;import java.util.List;
public record BillfordRecipe(String id,String displayName,int stock,int perPlayerLimit,Duration cooldown,List<BillfordIngredient>ingredients,List<BillfordReward>rewards){public BillfordRecipe{ingredients=List.copyOf(ingredients);rewards=List.copyOf(rewards);}}
