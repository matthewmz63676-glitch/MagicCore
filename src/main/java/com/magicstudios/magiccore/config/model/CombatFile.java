package com.magicstudios.magiccore.config.model;
import java.util.List;
public record CombatFile(int configVersion,long tagDurationSeconds,List<String>restrictedCommands,String logoutPolicy,
                         long enderPearlCooldownMillis,long tridentCooldownMillis,List<String>restrictedItems,
                         NewbieProtection newbieProtection, FastCrystal fastCrystal){
 public CombatFile{restrictedCommands=List.copyOf(restrictedCommands);restrictedItems=List.copyOf(restrictedItems);}
 public record NewbieProtection(boolean enabled,long durationSeconds,boolean removeOnAttack){}
 public record FastCrystal(boolean enabled,long cooldownMillis,double maximumRange,boolean requireLineOfSight,
                           List<String>worldAllowlist,List<String>baseBlocks,double damage,double knockback,
                           String sound,float soundVolume,float soundPitch){
  public FastCrystal{worldAllowlist=List.copyOf(worldAllowlist);baseBlocks=List.copyOf(baseBlocks);}
 }
}
