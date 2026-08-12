package com.magicstudios.magiccore.modules.shop;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellReceipt(UUID id,UUID quoteId,UUID playerId,String currency,List<SellLine> lines,long creditedMinor,
                          long balanceAfterMinor,UUID economyTransactionId,Instant createdAt){public SellReceipt{lines=List.copyOf(lines);}}
