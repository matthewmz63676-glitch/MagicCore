package com.magicstudios.magiccore.modules.crates;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ExternalCrateService implements CrateService {
    private final TransactionalDataStore store; private final ExternalCrateProvider provider;
    private final java.util.function.Consumer<Runnable>serverExecutor; private final DomainEventBus events; private final Clock clock;
    private final Map<String,CrateDefinition>definitions;
    private final RecordRepository<ExternalCrateOperation>operations=new RecordRepository<>("crates.external-operation",ExternalCrateOperation.class);
    public ExternalCrateService(TransactionalDataStore store,ExternalCrateProvider provider,java.util.function.Consumer<Runnable>serverExecutor,
                                DomainEventBus events,Clock clock,Map<String,CrateDefinition>definitions){
        if(!provider.available())throw new IllegalStateException("External crate provider is unavailable: "+provider.id());
        this.store=store;this.provider=provider;this.serverExecutor=serverExecutor;this.events=events;this.clock=clock;this.definitions=Map.copyOf(definitions);
    }
    @Override public Map<String,CrateDefinition>definitions(){return definitions;}
    @Override public CompletionStage<CrateKeyBalance>keyBalance(UUID playerId,String keyId){return onServer(()->new CrateKeyBalance(playerId,keyId,provider.keyBalance(playerId,keyId),clock.instant()));}
    @Override public CompletionStage<CrateKeyBalance>grantKeys(UUID playerId,String keyId,long amount,String operationKey){
        if(amount<1)throw new IllegalArgumentException("key amount must be positive");
        return prepare(operationKey,ExternalCrateOperation.Type.GRANT_KEYS,playerId,keyId,amount).thenCompose(state->{
            if(state.status()==ExternalCrateOperation.Status.COMPLETE)return keyBalance(playerId,keyId);
            if(state.detail().equals("EXISTING_PREPARED"))return CompletableFuture.failedFuture(new IllegalStateException("EXTERNAL_CRATE_RECONCILIATION_REQUIRED:"+operationKey));
            return onServer(()->{provider.grantKeys(playerId,keyId,amount);return null;}).thenCompose(ignored->complete(state,null,"GRANTED"))
                    .thenCompose(ignored->keyBalance(playerId,keyId));
        });
    }
    @Override public CompletionStage<CrateOpenResult>open(UUID playerId,String crateId,int amount,String operationKey){
        if(!definitions.containsKey(crateId))throw new IllegalArgumentException("unknown crate "+crateId);
        if(amount!=1)throw new IllegalArgumentException("EXTERNAL_CRATES_REQUIRE_SINGLE_OPEN");
        return prepare(operationKey,ExternalCrateOperation.Type.OPEN,playerId,crateId,amount).thenCompose(state->{
            if(state.status()==ExternalCrateOperation.Status.COMPLETE)return CompletableFuture.completedFuture(new CrateOpenResult(false,"REPLAY",state.opening()));
            if(state.detail().equals("EXISTING_PREPARED"))return CompletableFuture.completedFuture(new CrateOpenResult(false,"RECONCILIATION_REQUIRED",null));
            return onServer(()->provider.open(playerId,crateId)).thenCompose(opened->{
                if(!opened)return fail(state,"PROVIDER_REJECTED").thenApply(ignored->new CrateOpenResult(false,"PROVIDER_REJECTED",null));
                CrateOpening opening=new CrateOpening(UUID.randomUUID(),playerId,crateId,1,List.of(),0,clock.instant());
                return complete(state,opening,"OPENED").thenApply(ignored->{events.publish(new CrateOpened(playerId,crateId,1,opening.id(),operationKey,clock.instant()));return new CrateOpenResult(true,"OPENED",opening);});
            });
        });
    }
    @Override public CompletionStage<List<CrateOpening>>history(UUID playerId,int limit){if(limit<1||limit>100)throw new IllegalArgumentException("history limit must be 1..100");return store.read(reader->scanAll(reader).stream()
            .filter(value->value.type()==ExternalCrateOperation.Type.OPEN&&value.status()==ExternalCrateOperation.Status.COMPLETE&&value.playerId().equals(playerId)&&value.opening()!=null)
            .map(ExternalCrateOperation::opening).sorted(Comparator.comparing(CrateOpening::openedAt).reversed()).limit(limit).toList());}
    private CompletionStage<ExternalCrateOperation>prepare(String key,ExternalCrateOperation.Type type,UUID player,String subject,long amount){return store.transact("external-crate-prepare:"+key,tx->{
        var existing=operations.get(tx,key);if(existing.isPresent()){ExternalCrateOperation value=existing.get().value();if(value.type()!=type||!value.playerId().equals(player)||!value.subjectId().equals(subject)||value.amount()!=amount)throw new IllegalStateException("EXTERNAL_CRATE_OPERATION_CONFLICT");
            if(value.status()==ExternalCrateOperation.Status.PREPARED)return new ExternalCrateOperation(value.operationKey(),value.type(),value.status(),value.playerId(),value.subjectId(),value.amount(),value.opening(),"EXISTING_PREPARED",value.createdAt(),value.updatedAt());return value;}
        var now=clock.instant();ExternalCrateOperation value=new ExternalCrateOperation(key,type,ExternalCrateOperation.Status.PREPARED,player,subject,amount,null,"NEW",now,now);operations.put(tx,key,value,0);return value;});}
    private CompletionStage<ExternalCrateOperation>complete(ExternalCrateOperation before,CrateOpening opening,String detail){return transition(before,ExternalCrateOperation.Status.COMPLETE,opening,detail);}
    private CompletionStage<ExternalCrateOperation>fail(ExternalCrateOperation before,String detail){return transition(before,ExternalCrateOperation.Status.FAILED,null,detail);}
    private CompletionStage<ExternalCrateOperation>transition(ExternalCrateOperation before,ExternalCrateOperation.Status status,CrateOpening opening,String detail){return store.transact("external-crate-transition:"+before.operationKey()+":"+status,tx->{var current=operations.get(tx,before.operationKey()).orElseThrow();
        if(current.value().status()!=ExternalCrateOperation.Status.PREPARED)return current.value();ExternalCrateOperation updated=new ExternalCrateOperation(before.operationKey(),before.type(),status,before.playerId(),before.subjectId(),before.amount(),opening,detail,before.createdAt(),clock.instant());operations.put(tx,before.operationKey(),updated,current.revision());return updated;});}
    private<T>CompletionStage<T>onServer(CheckedSupplier<T>action){CompletableFuture<T>future=new CompletableFuture<>();serverExecutor.accept(()->{try{future.complete(action.get());}catch(Throwable failure){future.completeExceptionally(failure);}});return future;}
    private List<ExternalCrateOperation>scanAll(com.magicstudios.magiccore.storage.DataReader reader)throws Exception{ArrayList<ExternalCrateOperation>all=new ArrayList<>();String after=null;while(true){var page=operations.scanPage(reader,after,1000);page.forEach(value->all.add(value.value()));if(page.size()<1000)break;after=page.get(page.size()-1).key();}return all;}
    @FunctionalInterface private interface CheckedSupplier<T>{T get()throws Exception;}
}
