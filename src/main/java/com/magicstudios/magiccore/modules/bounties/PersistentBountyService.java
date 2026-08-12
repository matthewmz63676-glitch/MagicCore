package com.magicstudios.magiccore.modules.bounties;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import java.util.concurrent.CompletableFuture;
import com.magicstudios.magiccore.storage.DataReader;

public final class PersistentBountyService implements BountyService{
    private final TransactionalDataStore store;private final CurrencyDefinition currency;private final DomainEventBus events;
    private final Clock clock;private final long minimum,maximum;private final int taxBasisPoints;private final int maximumContributions;
    private final PlayerStatsService stats;private final PlayerProfileService profiles;private final boolean restrictionsEnabled;private final long minimumTargetPlaytime,minimumTargetKills;
    private final RecordRepository<Bounty>bounties=new RecordRepository<>("bounties.bounty",Bounty.class);
    private final RecordRepository<BountyContribution>contributions=new RecordRepository<>("bounties.contribution",BountyContribution.class);
    private final RecordRepository<BountyClaim>claims=new RecordRepository<>("bounties.claim",BountyClaim.class);
    public PersistentBountyService(TransactionalDataStore store,CurrencyDefinition currency,DomainEventBus events,Clock clock,
                                   long minimum,long maximum,int taxBasisPoints,int maximumContributions){
        this(store,currency,events,clock,minimum,maximum,taxBasisPoints,maximumContributions,null,null,false,0,0);
    }
    public PersistentBountyService(TransactionalDataStore store,CurrencyDefinition currency,DomainEventBus events,Clock clock,
                                   long minimum,long maximum,int taxBasisPoints,int maximumContributions,PlayerStatsService stats,PlayerProfileService profiles,
                                   boolean restrictionsEnabled,long minimumTargetPlaytime,long minimumTargetKills){
        if(minimum<1||maximum<minimum||taxBasisPoints<0||taxBasisPoints>10_000||maximumContributions<1||maximumContributions>1000)
            throw new IllegalArgumentException("Invalid bounty policy");
        this.store=store;this.currency=currency;this.events=events;this.clock=clock;this.minimum=minimum;this.maximum=maximum;
        this.taxBasisPoints=taxBasisPoints;this.maximumContributions=maximumContributions;
        this.stats=stats;this.profiles=profiles;this.restrictionsEnabled=restrictionsEnabled;this.minimumTargetPlaytime=minimumTargetPlaytime;this.minimumTargetKills=minimumTargetKills;
    }
    @Override public CompletionStage<BountyMutation> create(UUID creatorId,UUID targetId,long amount,String operationKey){
        if(creatorId.equals(targetId))throw new IllegalArgumentException("CANNOT_BOUNTY_SELF");
        if(amount<minimum||amount>maximum)throw new IllegalArgumentException("BOUNTY_AMOUNT_OUT_OF_RANGE");
        long tax=Math.multiplyExact(amount,taxBasisPoints)/10_000L;long debit=Math.addExact(amount,tax);
        CompletionStage<Void>eligibility=restrictionsEnabled?stats.stats(targetId).thenAccept(value->{if(value.playtimeSeconds()<minimumTargetPlaytime||value.kills()<minimumTargetKills)throw new IllegalStateException("TARGET_DOES_NOT_MEET_BOUNTY_REQUIREMENTS");}):CompletableFuture.completedFuture(null);
        return eligibility.thenCompose(ignored->store.transact("bounty-create:"+operationKey,tx->{
            var current=bounties.get(tx,targetId.toString());
            if(!IdempotencyKeys.reserve(tx,"bounty-create",operationKey))return new BountyMutation(false,"REPLAY",current.map(RecordRepository.VersionedValue::value).orElse(null),null,null);
            Bounty before=current.map(RecordRepository.VersionedValue::value).orElse(null);
            int count=before==null||before.status()!=Bounty.Status.ACTIVE?0:before.contributionCount();
            if(count>=maximumContributions)throw new IllegalStateException("BOUNTY_CONTRIBUTION_LIMIT");
            EconomyTransactionSupport.credit(tx,creatorId,currency,-debit,operationKey+":escrow",creatorId.toString(),"bounty-escrow-tax",clock.instant());
            var now=clock.instant();long total=Math.addExact(before==null||before.status()!=Bounty.Status.ACTIVE?0:before.totalEscrowMinor(),amount);
            Bounty updated=new Bounty(targetId,currency.id(),total,count+1,Bounty.Status.ACTIVE,null,null,
                    before==null||before.status()!=Bounty.Status.ACTIVE?now:before.createdAt(),now,null);
            bounties.put(tx,targetId.toString(),updated,current.map(RecordRepository.VersionedValue::revision).orElse(0L));
            BountyContribution contribution=new BountyContribution(UUID.randomUUID(),targetId,creatorId,amount,tax,
                    BountyContribution.Status.ACTIVE,operationKey,now,null);
            contributions.put(tx,contribution.id().toString(),contribution,0);
            return new BountyMutation(true,"CREATED",updated,contribution,null);
        }));
    }
    @Override public CompletionStage<BountyMutation> claim(com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill kill,String operationKey){
        return store.transact("bounty-claim:"+operationKey,tx->{
            var current=bounties.get(tx,kill.victimId().toString());
            if(!IdempotencyKeys.reserve(tx,"bounty-kill",kill.eventId().toString()))return new BountyMutation(false,"KILL_REPLAY",current.map(RecordRepository.VersionedValue::value).orElse(null),null,null);
            if(current.isEmpty()||current.get().value().status()!=Bounty.Status.ACTIVE)return new BountyMutation(false,"NO_ACTIVE_BOUNTY",null,null,null);
            Bounty bounty=current.get().value();EconomyTransactionSupport.credit(tx,kill.killerId(),currency,bounty.totalEscrowMinor(),
                    operationKey+":payout","bounty-escrow","bounty-claim",clock.instant());
            var now=clock.instant();Bounty closed=new Bounty(bounty.targetId(),bounty.currency(),bounty.totalEscrowMinor(),bounty.contributionCount(),
                    Bounty.Status.CLAIMED,kill.killerId(),kill.eventId(),bounty.createdAt(),now,now);
            bounties.put(tx,bounty.targetId().toString(),closed,current.get().revision());
            for(var record:scanAll(contributions,tx)){var contribution=record.value();
                if(contribution.targetId().equals(kill.victimId())&&contribution.status()==BountyContribution.Status.ACTIVE){
                    var claimed=new BountyContribution(contribution.id(),contribution.targetId(),contribution.creatorId(),contribution.escrowMinor(),contribution.taxMinor(),
                            BountyContribution.Status.CLAIMED,contribution.operationKey(),contribution.createdAt(),now);
                    contributions.put(tx,claimed.id().toString(),claimed,record.revision());}}
            BountyClaim claim=new BountyClaim(UUID.randomUUID(),kill.eventId(),kill.killerId(),kill.victimId(),bounty.totalEscrowMinor(),currency.id(),operationKey,kill.verifier(),now);
            claims.put(tx,claim.id().toString(),claim,0);return new BountyMutation(true,"CLAIMED",closed,null,claim);
        }).thenApply(result->{if(result.applied())events.publish(new BountyClaimed(result.bounty().targetId(),result.claim().killerId(),
                result.claim().payoutMinor(),result.claim().currency(),operationKey,clock.instant()));return result;});
    }
    @Override public CompletionStage<Optional<Bounty>> active(UUID targetId){return store.read(r->bounties.get(r,targetId.toString()).map(RecordRepository.VersionedValue::value).filter(b->b.status()==Bounty.Status.ACTIVE));}
    @Override public CompletionStage<List<Bounty>> leaderboard(int limit){return store.read(r->scanAll(bounties,r).stream().map(RecordRepository.KeyedVersionedValue::value)
            .filter(b->b.status()==Bounty.Status.ACTIVE).sorted(Comparator.comparingLong(Bounty::totalEscrowMinor).reversed()).limit(limit).toList());}
    @Override public CompletionStage<List<BountyClaim>> claimHistory(UUID playerId,int limit){return store.read(r->scanAll(claims,r).stream().map(RecordRepository.KeyedVersionedValue::value)
            .filter(c->c.killerId().equals(playerId)||c.victimId().equals(playerId)).sorted(Comparator.comparing(BountyClaim::claimedAt).reversed()).limit(limit).toList());}
    @Override public CompletionStage<BountyPage>search(String query,Sort sort,int page,int pageSize){if(page<0||pageSize<1||pageSize>100)throw new IllegalArgumentException("Invalid bounty page");String normalized=query==null?"":query.toLowerCase();
        return store.read(r->scanAll(bounties,r).stream().map(RecordRepository.KeyedVersionedValue::value).filter(b->b.status()==Bounty.Status.ACTIVE).toList()).thenCompose(values->{
            if(normalized.isBlank())return CompletableFuture.completedFuture(page(values,sort,page,pageSize));
            if(profiles==null)return CompletableFuture.completedFuture(page(values.stream().filter(b->b.targetId().toString().toLowerCase().contains(normalized)).toList(),sort,page,pageSize));
            var names=values.stream().collect(java.util.stream.Collectors.toMap(Bounty::targetId,b->profiles.find(b.targetId()).toCompletableFuture()));
            return CompletableFuture.allOf(names.values().toArray(CompletableFuture[]::new)).thenApply(ignored->{List<Bounty>filtered=values.stream().filter(b->b.targetId().toString().toLowerCase().contains(normalized)
                    ||names.get(b.targetId()).join().map(profile->profile.currentName().toLowerCase().contains(normalized)).orElse(false)).toList();return page(filtered,sort,page,pageSize);});});}
    private static BountyPage page(List<Bounty>values,Sort sort,int page,int size){Comparator<Bounty>comparator=switch(sort){case VALUE_DESC->Comparator.comparingLong(Bounty::totalEscrowMinor).reversed();case NEWEST->Comparator.comparing(Bounty::updatedAt).reversed();case CONTRIBUTIONS->Comparator.comparingInt(Bounty::contributionCount).reversed();};
        List<Bounty>sorted=values.stream().sorted(comparator.thenComparing(b->b.targetId().toString())).toList();int from=Math.min(sorted.size(),Math.multiplyExact(page,size)),to=Math.min(sorted.size(),from+size);return new BountyPage(sorted.subList(from,to),page,size,to<sorted.size());}
    private static<T>List<RecordRepository.KeyedVersionedValue<T>>scanAll(RecordRepository<T>repository,DataReader reader)throws Exception{
        java.util.ArrayList<RecordRepository.KeyedVersionedValue<T>>all=new java.util.ArrayList<>();String after=null;
        while(true){var page=repository.scanPage(reader,after,1000);all.addAll(page);if(page.size()<1000)break;after=page.get(page.size()-1).key();}
        return all;
    }
}
