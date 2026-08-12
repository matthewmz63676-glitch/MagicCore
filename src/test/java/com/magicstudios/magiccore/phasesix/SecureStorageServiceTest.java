package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.securestorage.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureStorageServiceTest {
    @Test void singleLeaseRevisionAndIdempotentCommitPreventDuplication() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"vault-test"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));var service=service(store,clock,PersistentSecureStorageService.NestedContainerPolicy.DENY_NON_EMPTY,PersistentSecureStorageService.CustomItemPolicy.ALLOW);UUID player=UUID.randomUUID();SecureStorageSession session=service.open(player,player,VirtualContainer.Type.VAULT,1,"open-1").toCompletableFuture().join();assertThat(session.container().size()).isEqualTo(18);assertThatThrownBy(()->service.open(player,player,VirtualContainer.Type.VAULT,1,"open-2").toCompletableFuture().join()).hasRootCauseMessage("STORAGE_ALREADY_OPEN");StoredItem item=item(0,false,false,false);StorageCommit first=service.save(player,session.lease().id(),session.revision(),List.of(item),"save-1").toCompletableFuture().join();assertThat(first.revision()).isEqualTo(1);assertThat(service.save(player,session.lease().id(),session.revision(),List.of(item),"save-1").toCompletableFuture().join().id()).isEqualTo(first.id());SecureStorageSession reopened=service.open(player,player,VirtualContainer.Type.VAULT,1,"open-3").toCompletableFuture().join();assertThat(reopened.revision()).isEqualTo(1);assertThat(reopened.container().items()).containsExactly(item);assertThatThrownBy(()->service.save(player,reopened.lease().id(),0,List.of(item),"stale").toCompletableFuture().join()).hasRootCauseMessage("STORAGE_SESSION_REVISION_MISMATCH");}finally{store.close();}}

    @Test void nestedCustomPayloadPoliciesAndRecoveryMailboxAreExplicit(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,32,"vault-policy"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));UUID player=UUID.randomUUID();var deny=service(store,clock,PersistentSecureStorageService.NestedContainerPolicy.DENY_NON_EMPTY,PersistentSecureStorageService.CustomItemPolicy.DENY);SecureStorageSession session=deny.open(player,player,VirtualContainer.Type.ENDER_CHEST,0,"open").toCompletableFuture().join();assertThatThrownBy(()->deny.save(player,session.lease().id(),0,List.of(item(0,true,true,false)),"nested")).isInstanceOf(IllegalArgumentException.class).hasMessage("NESTED_CONTAINER_POLICY_DENIED");assertThatThrownBy(()->deny.save(player,session.lease().id(),0,List.of(item(0,false,false,true)),"custom")).isInstanceOf(IllegalArgumentException.class).hasMessage("CUSTOM_ITEM_POLICY_DENIED");assertThat(deny.enqueueRecovery(player,session.lease().id(),List.of(item(0,false,false,false)),"recover-items").toCompletableFuture().join()).isTrue();assertThat(new PersistentDeliveryMailbox(store,clock).pending(player,10).toCompletableFuture().join()).singleElement().extracting(value->value.payloadType()).isEqualTo("magiccore/secure-storage-recovery-v1");}finally{store.close();}}

    @Test void expiredLeaseIsRecoveredAndContainerCanReopen(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,32,"vault-expiry"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));var service=service(store,clock,PersistentSecureStorageService.NestedContainerPolicy.ALLOW,PersistentSecureStorageService.CustomItemPolicy.ALLOW);UUID player=UUID.randomUUID();service.open(player,player,VirtualContainer.Type.VAULT,1,"first").toCompletableFuture().join();clock.advance(Duration.ofMinutes(3));assertThat(service.recoverExpired().toCompletableFuture().join()).isEqualTo(1);assertThat(service.open(player,player,VirtualContainer.Type.VAULT,1,"second").toCompletableFuture().join().lease().status()).isEqualTo(StorageLease.Status.OPEN);}finally{store.close();}}

    private static PersistentSecureStorageService service(InMemoryTransactionalDataStore store,Clock clock,PersistentSecureStorageService.NestedContainerPolicy nested,PersistentSecureStorageService.CustomItemPolicy custom){return new PersistentSecureStorageService(store,capabilities(),clock,Duration.ofMinutes(2),10,"VAULT_ROWS",1024,4096,nested,custom,"STORAGE_INSPECT");}
    private static CapabilityService capabilities(){return new CapabilityService(){public java.util.concurrent.CompletionStage<Boolean>has(UUID id,String capability){return CompletableFuture.completedFuture(true);}public java.util.concurrent.CompletionStage<Integer>limit(UUID id,String limit){return CompletableFuture.completedFuture(2);}public java.util.concurrent.CompletionStage<Boolean>canTarget(UUID actor,UUID target){return CompletableFuture.completedFuture(true);}};}
    private static StoredItem item(int slot,boolean container,boolean nonEmpty,boolean custom){return new StoredItem(slot,"minecraft:diamond",1,Base64.getEncoder().encodeToString(new byte[]{1,2,3}),container,nonEmpty,custom);}
    private static final class MutableClock extends Clock{private Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration duration){now=now.plus(duration);}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return this;}@Override public Instant instant(){return now;}}
}
