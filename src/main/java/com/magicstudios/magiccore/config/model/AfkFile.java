package com.magicstudios.magiccore.config.model;

import java.util.List;

public record AfkFile(int configVersion,Policy policy,Eligibility eligibility,List<Zone> zones) {
    public AfkFile { zones=List.copyOf(zones); }
    public record Policy(long intervalSeconds,long baseShards,long dailyCap,long reconnectProtectionSeconds,
                         long diminishingAfter,long diminishingBasisPoints) { }
    public record Eligibility(long minimumSessionSeconds,int minimumPresenceSamples,int minimumDistinctPositions,
                              int minimumLookChanges,int maximumMacroRiskBasisPoints) { }
    public record Zone(String id,String type,String world,String worldGuardRegion,
                       int minimumX,int minimumY,int minimumZ,int maximumX,int maximumY,int maximumZ) { }
}
