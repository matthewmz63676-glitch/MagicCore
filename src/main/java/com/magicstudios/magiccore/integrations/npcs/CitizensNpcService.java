package com.magicstudios.magiccore.integrations.npcs;

import com.magicstudios.magiccore.platform.SchedulerFacade;
import org.bukkit.entity.Entity;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class CitizensNpcService implements NpcIntegrationService {
    private final SchedulerFacade scheduler;private final Object registry;private final Method create;private final Method spawn,getEntity;private final Map<String,Object>npcs=new ConcurrentHashMap<>();private final Map<UUID,NpcAction>actions=new ConcurrentHashMap<>();
    private CitizensNpcService(SchedulerFacade scheduler,Object registry,Method create,Method spawn,Method getEntity){this.scheduler=scheduler;this.registry=registry;this.create=create;this.spawn=spawn;this.getEntity=getEntity;}
    public static NpcIntegrationService create(String provider,SchedulerFacade scheduler){if(!provider.equalsIgnoreCase("CITIZENS"))return unavailable(provider);try{Class<?>api=Class.forName("net.citizensnpcs.api.CitizensAPI");Object registry=api.getMethod("getNPCRegistry").invoke(null);Class<?>npc=Class.forName("net.citizensnpcs.api.npc.NPC");
        return new CitizensNpcService(scheduler,registry,registry.getClass().getMethod("createNPC",org.bukkit.entity.EntityType.class,String.class),npc.getMethod("spawn",org.bukkit.Location.class),npc.getMethod("getEntity"));}catch(ReflectiveOperationException failure){return unavailable(provider);}}
    private static NpcIntegrationService unavailable(String provider){return new NpcIntegrationService(){public String provider(){return provider;}public boolean available(){return false;}public CompletionStage<Boolean>upsert(NpcSpec s){return CompletableFuture.completedFuture(false);}public Optional<NpcAction>actionForEntity(UUID id){return Optional.empty();}};}
    @Override public String provider(){return "CITIZENS";}@Override public boolean available(){return true;}
    @Override public CompletionStage<Boolean>upsert(NpcSpec spec){CompletableFuture<Boolean>result=new CompletableFuture<>();scheduler.executeGlobal(()->{try{Object npc=npcs.computeIfAbsent(spec.id(),ignored->{try{return create.invoke(registry,spec.entityType(),spec.displayName());}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}});spawn.invoke(npc,spec.location());Entity entity=(Entity)getEntity.invoke(npc);if(entity!=null)actions.put(entity.getUniqueId(),spec.action());result.complete(true);}catch(Exception failure){result.completeExceptionally(failure);}});return result;}
    @Override public Optional<NpcAction>actionForEntity(UUID entityId){return Optional.ofNullable(actions.get(entityId));}
}
