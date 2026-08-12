package com.magicstudios.magiccore.integrations.items;

import com.magicstudios.magiccore.platform.SchedulerFacade;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ReflectiveCustomItemService implements CustomItemService {
    private final String provider;private final SchedulerFacade scheduler;private final Method lookup,build;
    private ReflectiveCustomItemService(String provider,SchedulerFacade scheduler,Method lookup,Method build){this.provider=provider;this.scheduler=scheduler;this.lookup=lookup;this.build=build;}
    public static CustomItemService create(String configured,SchedulerFacade scheduler){String provider=configured.toUpperCase();if(provider.equals("NONE"))return unavailable(provider);
        try{if(provider.equals("ITEMSADDER")){Class<?>stack=Class.forName("dev.lone.itemsadder.api.CustomStack");return new ReflectiveCustomItemService(provider,scheduler,stack.getMethod("getInstance",String.class),stack.getMethod("getItemStack"));}
            if(provider.equals("NEXO")){Class<?>items=Class.forName("com.nexomc.nexo.api.NexoItems");Method lookup=items.getMethod("itemFromId",String.class);return new ReflectiveCustomItemService(provider,scheduler,lookup,lookup.getReturnType().getMethod("build"));}}
        catch(ReflectiveOperationException failure){return unavailable(provider);}return unavailable(provider);}
    private static CustomItemService unavailable(String provider){return new CustomItemService(){public String provider(){return provider;}public boolean available(){return false;}public CompletionStage<Optional<ItemStack>>create(String id,int amount){return CompletableFuture.completedFuture(Optional.empty());}};}
    @Override public String provider(){return provider;}@Override public boolean available(){return true;}
    @Override public CompletionStage<Optional<ItemStack>>create(String itemId,int amount){if(amount<1)throw new IllegalArgumentException("amount must be positive");CompletableFuture<Optional<ItemStack>>result=new CompletableFuture<>();scheduler.executeGlobal(()->{try{Object value=lookup.invoke(null,itemId);if(value==null){result.complete(Optional.empty());return;}ItemStack item=(ItemStack)build.invoke(value);item.setAmount(amount);result.complete(Optional.of(item));}catch(ReflectiveOperationException failure){result.completeExceptionally(failure);}});return result;}
}
