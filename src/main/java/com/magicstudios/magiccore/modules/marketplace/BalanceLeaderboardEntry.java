package com.magicstudios.magiccore.modules.marketplace;
import java.util.UUID;
public record BalanceLeaderboardEntry(UUID playerId,String currency,long balanceMinor,int position){}
