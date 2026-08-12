package com.magicstudios.magiccore.modules.afk;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShardBalance(UUID playerId,long amount,long earnedToday,LocalDate earningDate,Instant updatedAt) { }
