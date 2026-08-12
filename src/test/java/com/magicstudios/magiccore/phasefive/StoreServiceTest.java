package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.modules.crates.PersistentCrateService;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.PersistentEconomyService;
import com.magicstudios.magiccore.modules.store.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.ranks.InternalRankService;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankDefinition;
import com.magicstudios.magiccore.ranks.RankType;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StoreServiceTest {
    @Test void signedPurchaseCheckpointsTypedActionsAndReplaysWithoutDuplicateFulfillment() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"store-test"));
        try{var clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);var events=new DomainEventBus();
            var coins=new CurrencyDefinition("COINS","Coins","$",0,0,1_000_000);var economy=new PersistentEconomyService(store,events,"COINS",Map.of("COINS",coins),clock);
            var crates=new PersistentCrateService(store,events,clock,Map.of(),Map.of("COINS",coins),"COINS",ignored->0);
            var ranks=new InternalRankService(store,events,catalog(),clock);
            var actions=List.of(new ProductAction(ProductAction.Type.CURRENCY,"COINS",500,"",0,"AIR",0,"",""),
                    new ProductAction(ProductAction.Type.CRATE_KEY,"",0,"VOTE_KEY",2,"AIR",0,"",""),
                    new ProductAction(ProductAction.Type.ITEM,"",0,"",0,"DIAMOND",3,"",""),
                    new ProductAction(ProductAction.Type.RANK,"",0,"",0,"AIR",0,"","VIP"));
            var product=new ProductDefinition("PACK","Pack",499,actions);String secret="test-secret";var service=new PersistentStoreService(store,economy,crates,ranks,events,clock,
                    "https://store.example.com",true,secret, Duration.ofDays(1),true,10_000,Map.of("PACK",product));
            AtomicInteger announcements=new AtomicInteger();events.subscribe("test",PurchaseFulfilled.class,event->announcements.incrementAndGet());UUID player=UUID.randomUUID();
            var request=new PurchaseRequest("evt-1","PACK",player,"Buyer",499,clock.instant(),"nonce-1");String signature=PurchaseSignatures.sign(request,secret);

            var first=service.accept(request,signature).toCompletableFuture().join();assertThat(first.accepted()).isTrue();assertThat(first.purchase().status()).isEqualTo(PurchaseRecord.Status.COMPLETE);
            assertThat(economy.balance(player,"COINS").toCompletableFuture().join().minorUnits()).isEqualTo(500);
            assertThat(crates.keyBalance(player,"VOTE_KEY").toCompletableFuture().join().amount()).isEqualTo(2);
            assertThat(ranks.rankOf(player).toCompletableFuture().join()).isEqualTo("VIP");assertThat(new PersistentDeliveryMailbox(store,clock).pending(player,10).toCompletableFuture().join()).hasSize(1);
            assertThat(service.donationGoal().toCompletableFuture().join().contributedMinor()).isEqualTo(499);assertThat(announcements).hasValue(1);

            var replay=service.accept(request,signature).toCompletableFuture().join();assertThat(replay.replay()).isTrue();
            assertThat(economy.balance(player,"COINS").toCompletableFuture().join().minorUnits()).isEqualTo(500);assertThat(crates.keyBalance(player,"VOTE_KEY").toCompletableFuture().join().amount()).isEqualTo(2);
        }finally{store.close();}
    }

    @Test void invalidSignatureIsRejectedBeforePersistence() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(1,16,"store-signature-test"));
        try{var clock=Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"),ZoneOffset.UTC);var events=new DomainEventBus();var coins=new CurrencyDefinition("COINS","Coins","$",0,0,1000);
            var economy=new PersistentEconomyService(store,events,"COINS",Map.of("COINS",coins),clock);var crates=new PersistentCrateService(store,events,clock,Map.of(),Map.of("COINS",coins),"COINS",ignored->0);
            var ranks=new InternalRankService(store,events,catalog(),clock);
            var product=new ProductDefinition("COINS","Coins",1,List.of(new ProductAction(ProductAction.Type.CURRENCY,"COINS",1,"",0,"AIR",0,"","")));
            var service=new PersistentStoreService(store,economy,crates,ranks,events,clock,"https://store.example.com",true,"secret",Duration.ofHours(1),false,100,Map.of("COINS",product));
            var request=new PurchaseRequest("evt-bad","COINS",UUID.randomUUID(),"Buyer",1,clock.instant(),"nonce");assertThat(service.accept(request,"bad").toCompletableFuture().join().code()).isEqualTo("INVALID_SIGNATURE");
        }finally{store.close();}
    }
    private static RankCatalog catalog(){java.util.LinkedHashMap<String,RankDefinition>definitions=new java.util.LinkedHashMap<>();definitions.put("DEFAULT",rank("DEFAULT",RankType.PLAYER,0));
        definitions.put("VIP",rank("VIP",RankType.DONOR,10));for(int i=2;i<=5;i++)definitions.put("DONOR_"+i,rank("DONOR_"+i,RankType.DONOR,10+i));
        for(int i=1;i<=5;i++)definitions.put("STAFF_"+i,rank("STAFF_"+i,RankType.STAFF,100+i));return new RankCatalog("DEFAULT",definitions);}
    private static RankDefinition rank(String id,RankType type,int weight){return new RankDefinition(id,type,id,weight,Set.of(),Set.of(),Set.of(),Map.of());}
}
