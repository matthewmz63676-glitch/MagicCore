package com.magicstudios.magiccore.modules.marketplace;
import com.magicstudios.magiccore.modules.auction.AuctionListing;
import com.magicstudios.magiccore.modules.bounties.Bounty;
import com.magicstudios.magiccore.modules.economy.EconomyTransaction;
import com.magicstudios.magiccore.modules.economy.WalletBalance;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.profiles.PlayerProfile;
import com.magicstudios.magiccore.modules.orders.BuyOrder;
import com.magicstudios.magiccore.storage.DataReader;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.Map;

public final class PersistentMarketplaceAnalyticsService implements MarketplaceAnalyticsService{
 private final TransactionalDataStore store;private final Clock clock;
 private final Map<String,CurrencyDefinition>currencies;
 private final RecordRepository<AuctionListing>auctions=new RecordRepository<>("auction.listing",AuctionListing.class);
 private final RecordRepository<BuyOrder>orders=new RecordRepository<>("orders.order",BuyOrder.class);
 private final RecordRepository<Bounty>bounties=new RecordRepository<>("bounties.bounty",Bounty.class);
 private final RecordRepository<EconomyTransaction>ledger=new RecordRepository<>("economy.ledger",EconomyTransaction.class);
 private final RecordRepository<WalletBalance>balances=new RecordRepository<>("economy.balance",WalletBalance.class);
 private final RecordRepository<PlayerProfile>profiles=new RecordRepository<>("profiles.player",PlayerProfile.class);
 public PersistentMarketplaceAnalyticsService(TransactionalDataStore store,Clock clock,Map<String,CurrencyDefinition>currencies){this.store=store;this.clock=clock;this.currencies=Map.copyOf(currencies);}
 @Override public CompletionStage<MarketplaceSnapshot> snapshot(){return store.read(reader->{var auction=all(auctions,reader);var order=all(orders,reader);var bounty=all(bounties,reader);var transactions=all(ledger,reader);
  long activeAuctions=auction.stream().filter(a->a.status()==AuctionListing.Status.ACTIVE).count();
  long activeValue=auction.stream().filter(a->a.status()==AuctionListing.Status.ACTIVE).mapToLong(AuctionListing::priceMinor).sum();
  long sold=auction.stream().filter(a->a.status()==AuctionListing.Status.SOLD).count();long soldVolume=auction.stream().filter(a->a.status()==AuctionListing.Status.SOLD).mapToLong(AuctionListing::priceMinor).sum();
  long openOrders=order.stream().filter(o->o.status()==BuyOrder.Status.OPEN).count();long orderEscrow=order.stream().filter(o->o.status()==BuyOrder.Status.OPEN).mapToLong(BuyOrder::escrowRemainingMinor).sum();
  long activeBounties=bounty.stream().filter(b->b.status()==Bounty.Status.ACTIVE).count();long bountyEscrow=bounty.stream().filter(b->b.status()==Bounty.Status.ACTIVE).mapToLong(Bounty::totalEscrowMinor).sum();
  long issued=transactions.stream().filter(t->t.type().equals("ISSUANCE")).mapToLong(EconomyTransaction::amountMinor).sum();
  long sunk=transactions.stream().filter(t->t.type().equals("SINK")).mapToLong(EconomyTransaction::amountMinor).sum();
  long transferred=transactions.stream().filter(t->t.type().equals("TRANSFER")).mapToLong(EconomyTransaction::amountMinor).sum();
  return new MarketplaceSnapshot(activeAuctions,activeValue,sold,soldVolume,openOrders,orderEscrow,activeBounties,bountyEscrow,transactions.size(),issued,sunk,transferred,clock.instant());});}
 @Override public CompletionStage<List<BalanceLeaderboardEntry>> balanceLeaderboard(String currency,int limit){if(limit<1||limit>100)throw new IllegalArgumentException("limit must be 1..100");
  return store.read(reader->{CurrencyDefinition definition=java.util.Optional.ofNullable(currencies.get(currency)).orElseThrow(()->new IllegalArgumentException("UNKNOWN_CURRENCY"));
   java.util.Map<java.util.UUID,WalletBalance>known=new java.util.LinkedHashMap<>();all(profiles,reader).forEach(profile->known.put(profile.playerId(),new WalletBalance(profile.playerId(),currency,definition.startingBalanceMinor())));
   all(balances,reader).stream().filter(b->b.currency().equals(currency)).forEach(balance->known.put(balance.playerId(),balance));
   List<WalletBalance> ranked=known.values().stream().sorted(Comparator.comparingLong(WalletBalance::minorUnits).reversed().thenComparing(WalletBalance::playerId)).limit(limit).toList();
   List<BalanceLeaderboardEntry>result=new ArrayList<>();for(int i=0;i<ranked.size();i++){var b=ranked.get(i);result.add(new BalanceLeaderboardEntry(b.playerId(),b.currency(),b.minorUnits(),i+1));}return List.copyOf(result);});}
 private static<T>List<T>all(RecordRepository<T>repository,DataReader reader)throws Exception{List<T>result=new ArrayList<>();String after=null;while(true){var page=repository.scanPage(reader,after,1000);page.forEach(v->result.add(v.value()));if(page.size()<1000)break;after=page.get(page.size()-1).key();}return List.copyOf(result);}
}
