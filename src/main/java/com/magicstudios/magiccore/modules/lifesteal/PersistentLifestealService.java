package com.magicstudios.magiccore.modules.lifesteal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentLifestealService implements LifestealService{
 public enum NonPlayerDeathPolicy{IGNORE,LOSE_HEART}
 private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
 private final TransactionalDataStore store;private final DomainEventBus events;private final Clock clock;
 private final int starting,minimum,maximum,revivalHearts;private final Duration antiFarmCooldown;
 private final NonPlayerDeathPolicy nonPlayerPolicy;private final String heartMaterial,heartDisplayName;
 private final RecordRepository<HeartAccount>accounts=new RecordRepository<>("lifesteal.account",HeartAccount.class);
 private final RecordRepository<HeartTransferPair>pairs=new RecordRepository<>("lifesteal.pair",HeartTransferPair.class);
 public PersistentLifestealService(TransactionalDataStore store,DomainEventBus events,Clock clock,int starting,int minimum,int maximum,
                                   int revivalHearts,Duration antiFarmCooldown,NonPlayerDeathPolicy nonPlayerPolicy,String heartMaterial,String heartDisplayName){
  if(minimum<1||starting<minimum||maximum<starting||revivalHearts<minimum||revivalHearts>maximum||antiFarmCooldown.isNegative())throw new IllegalArgumentException("Invalid heart policy");
  this.store=store;this.events=events;this.clock=clock;this.starting=starting;this.minimum=minimum;this.maximum=maximum;this.revivalHearts=revivalHearts;
  this.antiFarmCooldown=antiFarmCooldown;this.nonPlayerPolicy=nonPlayerPolicy;this.heartMaterial=heartMaterial;this.heartDisplayName=heartDisplayName;
 }
 @Override public CompletionStage<HeartAccount> account(UUID id){return store.read(r->load(r,id));}
 @Override public CompletionStage<HeartMutation> transfer(VerifiedPlayerKill kill,String operationKey){return store.transact("heart-transfer:"+operationKey,tx->{
  HeartAccount victim=load(tx,kill.victimId()),killer=load(tx,kill.killerId());
  if(!IdempotencyKeys.reserve(tx,"lifesteal-kill",kill.eventId().toString()))return new HeartMutation(false,"KILL_REPLAY",killer,victim);
  if(victim.eliminated())return new HeartMutation(false,"VICTIM_ALREADY_ELIMINATED",killer,victim);
  String pairKey=pairKey(kill.killerId(),kill.victimId());var prior=pairs.get(tx,pairKey);
  if(prior.isPresent()&&prior.get().value().lastTransferAt().plus(antiFarmCooldown).isAfter(kill.occurredAt()))return new HeartMutation(false,"ANTI_FARM_COOLDOWN",killer,victim);
  int victimHearts=victim.hearts()-1;boolean eliminated=victimHearts<minimum;boolean overflow=killer.hearts()>=maximum;
  HeartAccount updatedVictim=new HeartAccount(victim.playerId(),Math.max(0,victimHearts),eliminated,clock.instant());
  HeartAccount updatedKiller=new HeartAccount(killer.playerId(),overflow?killer.hearts():killer.hearts()+1,killer.eliminated(),clock.instant());
  put(tx,updatedVictim);put(tx,updatedKiller);HeartTransferPair pair=pair(kill.killerId(),kill.victimId(),clock.instant());
  pairs.put(tx,pairKey,pair,prior.map(RecordRepository.VersionedValue::revision).orElse(0L));
  if(overflow)enqueueHeart(tx,killer.playerId(),operationKey+":overflow");
  return new HeartMutation(true,eliminated?"ELIMINATED":overflow?"TRANSFERRED_TO_ITEM":"TRANSFERRED",updatedKiller,updatedVictim);
 }).thenApply(result->{if(result.applied()){boolean overflow=result.code().equals("TRANSFERRED_TO_ITEM");events.publish(new HeartTransferred(kill.killerId(),kill.victimId(),result.player().hearts(),result.other().hearts(),overflow,operationKey,clock.instant()));
   if(result.other().eliminated())events.publish(new PlayerEliminated(kill.victimId(),kill.killerId(),operationKey,clock.instant()));}return result;});}
 @Override public CompletionStage<HeartMutation> nonPlayerDeath(UUID playerId,String operationKey){if(nonPlayerPolicy==NonPlayerDeathPolicy.IGNORE)return account(playerId).thenApply(a->new HeartMutation(false,"IGNORED",a,null));
  return store.transact("heart-environment-death:"+operationKey,tx->{HeartAccount before=load(tx,playerId);if(!IdempotencyKeys.reserve(tx,"lifesteal-environment",operationKey))return new HeartMutation(false,"REPLAY",before,null);
   if(before.eliminated())return new HeartMutation(false,"ALREADY_ELIMINATED",before,null);int next=before.hearts()-1;HeartAccount updated=new HeartAccount(playerId,Math.max(0,next),next<minimum,clock.instant());put(tx,updated);return new HeartMutation(true,updated.eliminated()?"ELIMINATED":"LOST_HEART",updated,null);
  }).thenApply(result->{if(result.applied()&&result.player().eliminated())events.publish(new PlayerEliminated(playerId,null,operationKey,clock.instant()));return result;});}
 @Override public CompletionStage<HeartMutation> withdraw(UUID playerId,String operationKey){return store.transact("heart-withdraw:"+operationKey,tx->{HeartAccount before=load(tx,playerId);
  if(!IdempotencyKeys.reserve(tx,"heart-withdraw",operationKey))return new HeartMutation(false,"REPLAY",before,null);if(before.eliminated()||before.hearts()<=minimum)throw new IllegalStateException("MINIMUM_HEARTS_REACHED");
  HeartAccount updated=new HeartAccount(playerId,before.hearts()-1,false,clock.instant());put(tx,updated);enqueueHeart(tx,playerId,operationKey);return new HeartMutation(true,"WITHDRAWN",updated,null);});}
 @Override public CompletionStage<HeartMutation> consume(UUID playerId,String operationKey){return store.transact("heart-consume:"+operationKey,tx->{HeartAccount before=load(tx,playerId);
  if(!IdempotencyKeys.reserve(tx,"heart-consume",operationKey))return new HeartMutation(false,"REPLAY",before,null);if(before.eliminated())throw new IllegalStateException("ELIMINATED_REQUIRES_REVIVAL");if(before.hearts()>=maximum)throw new IllegalStateException("MAXIMUM_HEARTS_REACHED");
  HeartAccount updated=new HeartAccount(playerId,before.hearts()+1,false,clock.instant());put(tx,updated);return new HeartMutation(true,"CONSUMED",updated,null);});}
 @Override public CompletionStage<HeartMutation> revive(UUID playerId,String operationKey){return store.transact("heart-revive:"+operationKey,tx->{HeartAccount before=load(tx,playerId);
  if(!IdempotencyKeys.reserve(tx,"heart-revive",operationKey))return new HeartMutation(false,"REPLAY",before,null);if(!before.eliminated())throw new IllegalStateException("PLAYER_NOT_ELIMINATED");HeartAccount updated=new HeartAccount(playerId,revivalHearts,false,clock.instant());put(tx,updated);return new HeartMutation(true,"REVIVED",updated,null);
  }).thenApply(result->{if(result.applied())events.publish(new PlayerRevived(playerId,result.player().hearts(),operationKey,clock.instant()));return result;});}
 @Override public CompletionStage<List<HeartAccount>>leaderboard(int limit){return store.read(r->accounts.scan(r,null,1000).stream().map(RecordRepository.VersionedValue::value).sorted(Comparator.comparingInt(HeartAccount::hearts).reversed()).limit(limit).toList());}
 private HeartAccount load(com.magicstudios.magiccore.storage.DataReader reader,UUID id)throws Exception{return accounts.get(reader,id.toString()).map(RecordRepository.VersionedValue::value).orElse(new HeartAccount(id,starting,false,clock.instant()));}
 private void put(com.magicstudios.magiccore.storage.DataTransaction tx,HeartAccount account)throws Exception{var current=accounts.get(tx,account.playerId().toString());accounts.put(tx,account.playerId().toString(),account,current.map(RecordRepository.VersionedValue::revision).orElse(0L));}
 private void enqueueHeart(com.magicstudios.magiccore.storage.DataTransaction tx,UUID recipient,String operationKey)throws Exception{byte[]payload=JSON.writeValueAsBytes(new HeartItemPayload(heartMaterial,1,"HEART",heartDisplayName));DeliveryTransactionSupport.enqueue(tx,MailboxDelivery.pending(UUID.randomUUID(),recipient,operationKey,"magiccore/heart-v1",payload,clock.instant()));}
 private static String pairKey(UUID a,UUID b){return a.compareTo(b)<0?a+":"+b:b+":"+a;}
 private static HeartTransferPair pair(UUID a,UUID b,java.time.Instant at){return a.compareTo(b)<0?new HeartTransferPair(a,b,at):new HeartTransferPair(b,a,at);}
}
