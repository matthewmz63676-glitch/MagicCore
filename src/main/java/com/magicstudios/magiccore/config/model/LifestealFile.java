package com.magicstudios.magiccore.config.model;
import java.util.List;
import java.util.Map;
public record LifestealFile(int configVersion,int startingHearts,int minimumHearts,int maximumHearts,int revivalHearts,
                            long samePlayerCooldownSeconds,String nonPlayerDeathPolicy,String eliminationAction,
                            boolean revivalEnabled,HeartItem heartItem,Recipe recipe,HeartItem revivalItem,Recipe revivalRecipe){
 public LifestealFile{if(revivalItem==null)revivalItem=new HeartItem("TOTEM_OF_UNDYING","<gold>Revival Heart</gold>");
  if(revivalRecipe==null)revivalRecipe=new Recipe(false,List.of("GGG","GHG","GGG"),Map.of("G","GOLD_BLOCK","H","HEART_OF_THE_SEA"));}
 public record HeartItem(String material,String displayName){}
 public record Recipe(boolean enabled,List<String>shape,Map<String,String>ingredients){public Recipe{shape=List.copyOf(shape);ingredients=Map.copyOf(ingredients);}}
}
