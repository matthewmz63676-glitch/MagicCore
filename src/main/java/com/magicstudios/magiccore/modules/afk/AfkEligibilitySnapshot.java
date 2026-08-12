package com.magicstudios.magiccore.modules.afk;

public record AfkEligibilitySnapshot(String zoneId,long sessionSeconds,long secondsSinceReconnect,
                                     int presenceSamples,int distinctPositions,int lookChanges,
                                     int macroRiskBasisPoints) { }
