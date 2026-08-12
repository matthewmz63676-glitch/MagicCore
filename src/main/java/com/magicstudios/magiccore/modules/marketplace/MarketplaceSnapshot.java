package com.magicstudios.magiccore.modules.marketplace;
import java.time.Instant;
public record MarketplaceSnapshot(long activeAuctions,long activeAuctionValueMinor,long soldAuctions,long soldAuctionVolumeMinor,
                                  long openOrders,long orderEscrowMinor,long activeBounties,long bountyEscrowMinor,
                                  long economyTransactions,long issuedMinor,long sunkMinor,long transferredMinor,Instant capturedAt){}
