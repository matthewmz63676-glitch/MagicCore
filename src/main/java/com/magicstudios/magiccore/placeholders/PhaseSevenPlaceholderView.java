package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.events.*;
import java.util.concurrent.CompletionStage;

public final class PhaseSevenPlaceholderView {
    private final KothService koth;private final VotePartyService votes;private volatile String kothStatus="INACTIVE";private volatile int voteCount;private volatile int pinataRemaining;
    public PhaseSevenPlaceholderView(KothService koth,VotePartyService votes){this.koth=koth;this.votes=votes;}
    public void register(String owner,PlaceholderRegistry registry,DomainEventBus events){registry.register(owner,"koth_status",ignored->kothStatus);registry.register(owner,"vote_party_count",ignored->Integer.toString(voteCount));registry.register(owner,"pinata_remaining_hits",ignored->Integer.toString(pinataRemaining));events.subscribe(owner,KothChanged.class,event->kothStatus=event.status().equals("ACTIVE")?(event.holder().isBlank()?event.definitionId()+":ACTIVE":event.definitionId()+":"+event.holder()):event.status());events.subscribe(owner,VoteVerified.class,event->refreshVotes());events.subscribe(owner,PinataChanged.class,event->pinataRemaining=Math.max(0,event.maximumHits()-event.hits()));}
    public CompletionStage<Void>refresh(){return java.util.concurrent.CompletableFuture.allOf(refreshVotes().toCompletableFuture(),votes.activeParty().thenAccept(value->pinataRemaining=value.map(PinataParty::remaining).orElse(0)).toCompletableFuture());}
    private CompletionStage<Void>refreshVotes(){return votes.state().thenAccept(value->voteCount=value.count());}
}
