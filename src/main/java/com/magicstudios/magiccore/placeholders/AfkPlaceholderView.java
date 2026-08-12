package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.afk.ShardsChanged;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class AfkPlaceholderView {
    private final ShardService shards;private final Map<UUID,Long>cache=new ConcurrentHashMap<>();
    public AfkPlaceholderView(ShardService shards){this.shards=shards;}
    public void register(String owner,PlaceholderRegistry registry,DomainEventBus events){registry.register(owner,"afk_shards",context->{Long value=context.subjectId()==null?null:cache.get(context.subjectId());return value==null?"":Long.toString(value);});events.subscribe(owner,ShardsChanged.class,event->cache.put(event.playerId(),event.amount()));}
    public CompletionStage<Void>refresh(UUID playerId){return shards.balance(playerId).thenAccept(value->cache.put(playerId,value.amount()));}
}
