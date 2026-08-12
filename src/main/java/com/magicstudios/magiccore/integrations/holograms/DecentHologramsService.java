package com.magicstudios.magiccore.integrations.holograms;

import com.magicstudios.magiccore.platform.SchedulerFacade;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class DecentHologramsService implements HologramIntegrationService {
    private final SchedulerFacade scheduler;private final Method get,create,setLines,move,remove;
    private DecentHologramsService(SchedulerFacade scheduler,Method get,Method create,Method setLines,Method move,Method remove){this.scheduler=scheduler;this.get=get;this.create=create;this.setLines=setLines;this.move=move;this.remove=remove;}
    public static HologramIntegrationService create(String provider,SchedulerFacade scheduler){if(!provider.equalsIgnoreCase("DECENT_HOLOGRAMS"))return unavailable(provider);try{Class<?>api=Class.forName("eu.decentsoftware.holograms.api.DHAPI");Class<?>hologram=Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
        return new DecentHologramsService(scheduler,api.getMethod("getHologram",String.class),api.getMethod("createHologram",String.class,org.bukkit.Location.class,List.class),api.getMethod("setHologramLines",hologram,List.class),api.getMethod("moveHologram",hologram,org.bukkit.Location.class),api.getMethod("removeHologram",String.class));}
        catch(ReflectiveOperationException failure){return unavailable(provider);}}
    private static HologramIntegrationService unavailable(String provider){return new HologramIntegrationService(){public String provider(){return provider;}public boolean available(){return false;}public CompletionStage<Boolean>upsert(HologramSpec s){return CompletableFuture.completedFuture(false);}public CompletionStage<Boolean>remove(String id){return CompletableFuture.completedFuture(false);}};}
    @Override public String provider(){return "DECENT_HOLOGRAMS";}@Override public boolean available(){return true;}
    @Override public CompletionStage<Boolean>upsert(HologramSpec spec){CompletableFuture<Boolean>result=new CompletableFuture<>();scheduler.executeGlobal(()->{try{List<String>lines=spec.lines().stream().map(LegacyComponentSerializer.legacyAmpersand()::serialize).toList();Object current=get.invoke(null,spec.id());
        if(current==null)create.invoke(null,spec.id(),spec.location(),lines);else{setLines.invoke(null,current,lines);move.invoke(null,current,spec.location());}result.complete(true);}catch(ReflectiveOperationException failure){result.completeExceptionally(failure);}});return result;}
    @Override public CompletionStage<Boolean>remove(String id){CompletableFuture<Boolean>result=new CompletableFuture<>();scheduler.executeGlobal(()->{try{remove.invoke(null,id);result.complete(true);}catch(ReflectiveOperationException failure){result.completeExceptionally(failure);}});return result;}
}
