package com.magicstudios.magiccore.modules.afk;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.config.model.AfkFile;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentShardService implements ShardService {
    private final TransactionalDataStore store;private final DomainEventBus events;private final Clock clock;private final AfkFile.Policy policy;private final AfkFile.Eligibility requirements;private final Set<String>zones;
    private final RecordRepository<ShardBalance>balances=new RecordRepository<>("afk.balance",ShardBalance.class);private final RecordRepository<ShardTransaction>transactions=new RecordRepository<>("afk.transaction",ShardTransaction.class);
    public PersistentShardService(TransactionalDataStore store,DomainEventBus events,Clock clock,AfkFile.Policy policy,AfkFile.Eligibility requirements,Set<String>zones){this.store=store;this.events=events;this.clock=clock;this.policy=policy;this.requirements=requirements;this.zones=Set.copyOf(zones);}
    @Override public CompletionStage<ShardBalance>balance(UUID playerId){return store.read(reader->current(reader,playerId));}
    @Override public CompletionStage<ShardAwardResult>award(UUID playerId,AfkEligibilitySnapshot eligibility,String intervalId){String failure=eligibilityFailure(eligibility);if(failure!=null)return balance(playerId).thenApply(value->new ShardAwardResult(false,failure,0,value));String operationKey=playerId+":"+intervalId;
        return store.transact("afk-award:"+operationKey,tx->{ShardBalance before=current(tx,playerId);if(!IdempotencyKeys.reserve(tx,"afk-award",operationKey))return new ShardAwardResult(false,"REPLAY",0,before);ShardBalance today=normalizeDay(before);long remaining=Math.max(0,policy.dailyCap()-today.earnedToday());if(remaining==0)return new ShardAwardResult(false,"DAILY_CAP",0,today);
            long configured=today.earnedToday()>=policy.diminishingAfter()?Math.max(1,Math.multiplyExact(policy.baseShards(),policy.diminishingBasisPoints())/10_000L):policy.baseShards();long award=Math.min(remaining,configured);ShardBalance updated=new ShardBalance(playerId,Math.addExact(today.amount(),award),today.earnedToday()+award,today.earningDate(),clock.instant());put(tx,updated);record(tx,updated,award,"AFK:"+eligibility.zoneId(),operationKey);return new ShardAwardResult(true,"AWARDED",award,updated);
        }).thenApply(result->{if(result.applied())events.publish(new ShardsChanged(playerId,result.balance().amount(),result.awarded(),"AFK",operationKey,clock.instant()));return result;});}
    @Override public CompletionStage<ShardBalance>adjust(UUID playerId,long delta,String reason,String operationKey){if(reason==null||reason.isBlank())throw new IllegalArgumentException("Shard adjustment reason is required");return store.transact("shard-adjust:"+operationKey,tx->{ShardBalance before=normalizeDay(current(tx,playerId));if(!IdempotencyKeys.reserve(tx,"shard-adjust",operationKey))return before;long after=Math.addExact(before.amount(),delta);if(after<0)throw new IllegalStateException("INSUFFICIENT_SHARDS");ShardBalance updated=new ShardBalance(playerId,after,before.earnedToday(),before.earningDate(),clock.instant());put(tx,updated);record(tx,updated,delta,reason,operationKey);return updated;}).thenApply(updated->{events.publish(new ShardsChanged(playerId,updated.amount(),delta,reason,operationKey,clock.instant()));return updated;});}
    @Override public CompletionStage<List<ShardTransaction>>history(UUID playerId,int limit){if(limit<1||limit>100)throw new IllegalArgumentException("history limit must be 1..100");return store.read(reader->scanAll(reader).stream().filter(value->value.playerId().equals(playerId)).sorted(Comparator.comparing(ShardTransaction::occurredAt).reversed()).limit(limit).toList());}
    private String eligibilityFailure(AfkEligibilitySnapshot value){if(!zones.contains(value.zoneId()))return"OUTSIDE_AFK_ZONE";if(value.sessionSeconds()<requirements.minimumSessionSeconds())return"SESSION_TOO_SHORT";if(value.secondsSinceReconnect()<policy.reconnectProtectionSeconds())return"RECONNECT_PROTECTION";if(value.presenceSamples()<requirements.minimumPresenceSamples())return"INSUFFICIENT_PRESENCE";if(value.distinctPositions()<requirements.minimumDistinctPositions())return"INSUFFICIENT_ACTIVITY";if(value.lookChanges()<requirements.minimumLookChanges())return"INSUFFICIENT_LOOK_CHANGES";if(value.macroRiskBasisPoints()>requirements.maximumMacroRiskBasisPoints())return"MACRO_RISK";return null;}
    private ShardBalance current(com.magicstudios.magiccore.storage.DataReader reader,UUID player)throws Exception{return balances.get(reader,player.toString()).map(RecordRepository.VersionedValue::value).orElse(new ShardBalance(player,0,0,today(),clock.instant()));}
    private ShardBalance normalizeDay(ShardBalance value){return value.earningDate().equals(today())?value:new ShardBalance(value.playerId(),value.amount(),0,today(),clock.instant());}
    private LocalDate today(){return LocalDate.ofInstant(clock.instant(),ZoneOffset.UTC);}private void put(com.magicstudios.magiccore.storage.DataTransaction tx,ShardBalance value)throws Exception{var current=balances.get(tx,value.playerId().toString());balances.put(tx,value.playerId().toString(),value,current.map(RecordRepository.VersionedValue::revision).orElse(0L));}
    private void record(com.magicstudios.magiccore.storage.DataTransaction tx,ShardBalance after,long delta,String reason,String operation)throws Exception{ShardTransaction value=new ShardTransaction(UUID.randomUUID(),after.playerId(),delta,after.amount(),reason,operation,clock.instant());transactions.put(tx,value.id().toString(),value,0);}
    private List<ShardTransaction>scanAll(com.magicstudios.magiccore.storage.DataReader reader)throws Exception{ArrayList<ShardTransaction>all=new ArrayList<>();String after=null;while(true){var page=transactions.scanPage(reader,after,1000);page.forEach(value->all.add(value.value()));if(page.size()<1000)break;after=page.get(page.size()-1).key();}return all;}
}
