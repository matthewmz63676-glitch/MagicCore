package com.magicstudios.magiccore.modules.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.Money;
import com.magicstudios.magiccore.ranks.RankService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PersistentStoreService implements StoreService {
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    private final TransactionalDataStore store;private final EconomyService economy;private final CrateService crates;private final RankService ranks;
    private final DomainEventBus events;private final Clock clock;private final String url,secret;private final boolean enabled,goalEnabled;
    private final Duration maximumAge;private final long goalTarget;private final Map<String,ProductDefinition>products;
    private final RecordRepository<PurchaseRecord>purchases=new RecordRepository<>("store.purchase",PurchaseRecord.class);
    private final RecordRepository<DonationGoalState>goals=new RecordRepository<>("store.goal",DonationGoalState.class);

    public PersistentStoreService(TransactionalDataStore store,EconomyService economy,CrateService crates,RankService ranks,DomainEventBus events,
                                  Clock clock,String url,boolean enabled,String secret,Duration maximumAge,boolean goalEnabled,long goalTarget,
                                  Map<String,ProductDefinition>products){this.store=store;this.economy=economy;this.crates=crates;this.ranks=ranks;this.events=events;
        this.clock=clock;this.url=url;this.enabled=enabled;this.secret=secret;this.maximumAge=maximumAge;this.goalEnabled=goalEnabled;this.goalTarget=goalTarget;this.products=Map.copyOf(products);validate();}
    @Override public String url(){return url;}
    @Override public Map<String,ProductDefinition>products(){return products;}
    @Override public CompletionStage<PurchaseResult>accept(PurchaseRequest request,String signature){if(!enabled)return CompletableFuture.completedFuture(new PurchaseResult(false,false,"PURCHASES_DISABLED",null));
        ProductDefinition product=products.get(request.productId());if(product==null)return CompletableFuture.completedFuture(new PurchaseResult(false,false,"UNKNOWN_PRODUCT",null));
        if(request.paidMinor()<product.minimumPaidMinor())return CompletableFuture.completedFuture(new PurchaseResult(false,false,"UNDERPAID",null));
        var now=clock.instant();if(request.occurredAt().isBefore(now.minus(maximumAge))||request.occurredAt().isAfter(now.plus(Duration.ofMinutes(5))))return CompletableFuture.completedFuture(new PurchaseResult(false,false,"STALE_SIGNATURE",null));
        if(secret==null||secret.isBlank()||!PurchaseSignatures.verify(request,secret,signature))return CompletableFuture.completedFuture(new PurchaseResult(false,false,"INVALID_SIGNATURE",null));
        return initialize(request).thenCompose(record->{if(record.status()==PurchaseRecord.Status.COMPLETE)return announce(record).thenApply(ignored->new PurchaseResult(true,true,"REPLAY",record));
            return process(record,product).thenCompose(completed->announce(completed).thenApply(ignored->new PurchaseResult(true,false,"FULFILLED",completed)));});}
    @Override public CompletionStage<DonationGoalState>donationGoal(){return store.read(reader->goals.get(reader,"current").map(RecordRepository.VersionedValue::value).orElse(new DonationGoalState(0,goalTarget,clock.instant())));}

    private CompletionStage<PurchaseRecord>initialize(PurchaseRequest request){return store.transact("store-receive:"+request.eventId(),tx->{var existing=purchases.get(tx,request.eventId());if(existing.isPresent()){
            PurchaseRecord value=existing.get().value();if(!value.productId().equals(request.productId())||!value.playerId().equals(request.playerId())||value.paidMinor()!=request.paidMinor())throw new IllegalStateException("EVENT_ID_CONFLICT");return value;}
        PurchaseRecord value=new PurchaseRecord(request.eventId(),request.productId(),request.playerId(),request.playerName(),request.paidMinor(),0,PurchaseRecord.Status.PROCESSING,clock.instant(),null);purchases.put(tx,request.eventId(),value,0);return value;});}
    private CompletionStage<PurchaseRecord>process(PurchaseRecord current,ProductDefinition product){if(current.status()==PurchaseRecord.Status.COMPLETE)return CompletableFuture.completedFuture(current);
        int index=current.nextAction();if(index>=product.actions().size())return checkpoint(current.eventId(),index,product.actions().size());
        return apply(current,product.actions().get(index),index).thenCompose(ignored->checkpoint(current.eventId(),index,product.actions().size())).thenCompose(updated->process(updated,product));}
    private CompletionStage<?>apply(PurchaseRecord purchase,ProductAction action,int index){String operation="store:"+purchase.eventId()+":"+index;return switch(action.type()){
        case CURRENCY->economy.adjust(purchase.playerId(),new Money(action.currency(),action.amountMinor()),"STORE",purchase.productId(),operation);
        case CRATE_KEY->crates.grantKeys(purchase.playerId(),action.keyId(),action.keyAmount(),operation);
        case RANK->ranks.setRank(purchase.playerId(),action.rankId(),"STORE:"+purchase.productId(),operation);
        case ITEM->enqueueItem(purchase.playerId(),action,operation);};}
    private CompletionStage<Boolean>enqueueItem(UUID playerId,ProductAction action,String operation){return store.transact("store-item:"+operation,tx->{if(!IdempotencyKeys.reserve(tx,"store-item",operation))return false;
        byte[]payload=JSON.writeValueAsBytes(List.of(new StoreItemPayload(action.material(),action.amount(),action.itemDataBase64())));DeliveryTransactionSupport.enqueue(tx,MailboxDelivery.pending(UUID.randomUUID(),playerId,operation,"magiccore/store-items-v1",payload,clock.instant()));return true;});}
    private CompletionStage<PurchaseRecord>checkpoint(String eventId,int completedIndex,int actionCount){return store.transact("store-checkpoint:"+eventId+":"+completedIndex,tx->{var existing=purchases.get(tx,eventId).orElseThrow();PurchaseRecord current=existing.value();
        if(current.status()==PurchaseRecord.Status.COMPLETE||current.nextAction()>completedIndex)return current;int next=completedIndex+1;boolean complete=next>=actionCount;PurchaseRecord updated=new PurchaseRecord(current.eventId(),current.productId(),current.playerId(),current.playerName(),current.paidMinor(),next,
                complete?PurchaseRecord.Status.COMPLETE:PurchaseRecord.Status.PROCESSING,current.receivedAt(),complete?clock.instant():null);purchases.put(tx,eventId,updated,existing.revision());
        if(complete&&goalEnabled){var goal=goals.get(tx,"current");DonationGoalState before=goal.map(RecordRepository.VersionedValue::value).orElse(new DonationGoalState(0,goalTarget,clock.instant()));DonationGoalState after=new DonationGoalState(Math.addExact(before.contributedMinor(),current.paidMinor()),goalTarget,clock.instant());goals.put(tx,"current",after,goal.map(RecordRepository.VersionedValue::revision).orElse(0L));}return updated;});}
    private CompletionStage<Boolean>claimAnnouncement(PurchaseRecord purchase){return store.transact("store-announce:"+purchase.eventId(),tx->IdempotencyKeys.reserve(tx,"store-announcement",purchase.eventId()));}
    private CompletionStage<Void>announce(PurchaseRecord purchase){return claimAnnouncement(purchase).thenAccept(claimed->{if(claimed)events.publish(new PurchaseFulfilled(purchase.eventId(),purchase.productId(),purchase.playerId(),purchase.playerName(),purchase.paidMinor(),clock.instant()));});}
    private void validate(){if(url==null||url.isBlank()||maximumAge.isNegative()||maximumAge.isZero()||goalTarget<1)throw new IllegalArgumentException("invalid store policy");for(ProductDefinition product:products.values()){
        if(!product.id().matches("[A-Z][A-Z0-9_]*")||product.minimumPaidMinor()<0||product.actions().isEmpty())throw new IllegalArgumentException("invalid product "+product.id());for(ProductAction action:product.actions())switch(action.type()){
            case CURRENCY->{if(action.amountMinor()<1||!economy.currencies().containsKey(action.currency()))throw new IllegalArgumentException("invalid currency product action");}
            case CRATE_KEY->{if(action.keyId()==null||action.keyId().isBlank()||action.keyAmount()<1)throw new IllegalArgumentException("invalid crate key product action");}
            case ITEM->{if(action.material()==null||!action.material().matches("[A-Z0-9_]+")||action.amount()<1)throw new IllegalArgumentException("invalid item product action");}
            case RANK->ranks.catalog().require(action.rankId());}}}
}
