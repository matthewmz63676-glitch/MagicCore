package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.BalanceChanged;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class GemBalancePlaceholderView {
    private final EconomyService economy; private final String currency; private final Map<UUID,Long> cache=new ConcurrentHashMap<>();
    public GemBalancePlaceholderView(EconomyService economy,String currency){this.economy=economy;this.currency=currency;}
    public void register(String owner,PlaceholderRegistry registry,DomainEventBus events){registry.register(owner,"gems",context->context.subject().map(cache::get).map(String::valueOf).orElse(""));events.subscribe(owner,BalanceChanged.class,event->{if(event.currency().equals(currency))refresh(event.playerId());});}
    public CompletionStage<Void>refresh(UUID playerId){return economy.balance(playerId,currency).thenAccept(value->cache.put(playerId,value.minorUnits()));}
}
