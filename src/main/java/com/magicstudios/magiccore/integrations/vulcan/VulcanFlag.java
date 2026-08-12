package com.magicstudios.magiccore.integrations.vulcan;

import java.time.Instant;
import java.util.UUID;

public record VulcanFlag(UUID playerId,String check,double violationLevel,String detail,Instant observedAt) { }
