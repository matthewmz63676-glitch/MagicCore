package com.magicstudios.magiccore.config.model;

import java.util.List;

public record PlayerWarpsFile(int configVersion,List<String>categories,long defaultExpirySeconds,
                              Sponsorship sponsorship){public PlayerWarpsFile{categories=List.copyOf(categories);}
 public record Sponsorship(boolean enabled,String currency,long pricePerHourMinor,long minimumDurationSeconds,
                           long maximumDurationSeconds,int maximumActiveGlobal,int maximumActivePerPlayer){}
}
