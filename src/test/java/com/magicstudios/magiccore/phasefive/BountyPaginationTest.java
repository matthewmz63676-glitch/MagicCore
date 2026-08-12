package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.bounties.*;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import com.magicstudios.magiccore.storage.RecordRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BountyPaginationTest {
    @Test void searchAndClaimTraverseEveryStoragePage(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"bounty-pages"));try{
        Instant now=Instant.parse("2026-08-10T00:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);var bountyRecords=new RecordRepository<>("bounties.bounty",Bounty.class);var contributions=new RecordRepository<>("bounties.contribution",BountyContribution.class);
        UUID claimedTarget=id(2001);store.transact("seed-bounty-pages",tx->{for(int i=0;i<1001;i++){UUID target=id(i);bountyRecords.put(tx,target.toString(),new Bounty(target,"COINS",100+i,1,Bounty.Status.ACTIVE,null,null,now,now,null),0);}
            bountyRecords.put(tx,claimedTarget.toString(),new Bounty(claimedTarget,"COINS",500,1001,Bounty.Status.ACTIVE,null,null,now,now,null),0);
            for(int i=0;i<1001;i++){UUID contributionId=id(3000+i);contributions.put(tx,contributionId.toString(),new BountyContribution(contributionId,claimedTarget,id(5000+i),1,0,BountyContribution.Status.ACTIVE,"seed-"+i,now,null),0);}return null;}).toCompletableFuture().join();
        var coins=new CurrencyDefinition("COINS","Coins","$",0,1000,1_000_000);var service=new PersistentBountyService(store,coins,new DomainEventBus(),clock,1,10_000,0,1000);
        var page=service.search(id(1000).toString(),BountyService.Sort.VALUE_DESC,0,10).toCompletableFuture().join();assertThat(page.bounties()).singleElement().extracting(Bounty::targetId).isEqualTo(id(1000));
        UUID killer=id(9000),killId=id(9001);assertThat(service.claim(new VerifiedPlayerKill(killId,killer,claimedTarget,"TEST",now),"claim-all-pages").toCompletableFuture().join().applied()).isTrue();
        long active=store.read(reader->{long count=0;String after=null;while(true){var values=contributions.scanPage(reader,after,1000);count+=values.stream().filter(value->value.value().targetId().equals(claimedTarget)&&value.value().status()==BountyContribution.Status.ACTIVE).count();if(values.size()<1000)break;after=values.get(values.size()-1).key();}return count;}).toCompletableFuture().join();assertThat(active).isZero();
    }finally{store.close();}}
    private static UUID id(int value){return UUID.fromString("00000000-0000-0000-0000-"+String.format("%012d",value));}
}
