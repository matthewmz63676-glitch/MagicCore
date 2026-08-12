package com.magicstudios.magiccore.integrations.discord;

import com.magicstudios.magiccore.platform.SchedulerFacade;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class DiscordSrvIntegrationService implements DiscordIntegrationService {
    private final SchedulerFacade scheduler;private final Object plugin;private final Method accountManager,mainChannel;
    private DiscordSrvIntegrationService(SchedulerFacade scheduler,Object plugin,Method accountManager,Method mainChannel){this.scheduler=scheduler;this.plugin=plugin;this.accountManager=accountManager;this.mainChannel=mainChannel;}
    public static DiscordIntegrationService create(String provider,SchedulerFacade scheduler){if(!provider.equalsIgnoreCase("DISCORDSRV"))return unavailable(provider);try{Class<?>type=Class.forName("github.scarsz.discordsrv.DiscordSRV");Object plugin=type.getMethod("getPlugin").invoke(null);
        return new DiscordSrvIntegrationService(scheduler,plugin,type.getMethod("getAccountLinkManager"),type.getMethod("getMainTextChannel"));}catch(ReflectiveOperationException failure){return unavailable(provider);}}
    private static DiscordIntegrationService unavailable(String provider){return new DiscordIntegrationService(){public String provider(){return provider;}public boolean available(){return false;}
        public CompletionStage<Optional<String>>linkedDiscordId(UUID id){return CompletableFuture.completedFuture(Optional.empty());}public CompletionStage<Boolean>notify(String message){return CompletableFuture.completedFuture(false);}};}
    @Override public String provider(){return "DISCORDSRV";}@Override public boolean available(){return true;}
    @Override public CompletionStage<Optional<String>>linkedDiscordId(UUID playerId){return scheduler.supplyAsync(()->{Object manager=accountManager.invoke(plugin);Object id=manager.getClass().getMethod("getDiscordId",UUID.class).invoke(manager,playerId);return Optional.ofNullable((String)id);});}
    @Override public CompletionStage<Boolean>notify(String message){return scheduler.supplyAsync(()->{Object channel=mainChannel.invoke(plugin);if(channel==null)return false;Method send=null;for(Method candidate:channel.getClass().getMethods())if(candidate.getName().equals("sendMessage")&&candidate.getParameterCount()==1&&candidate.getParameterTypes()[0].isAssignableFrom(String.class)){send=candidate;break;}if(send==null)throw new NoSuchMethodException(channel.getClass().getName()+"#sendMessage(String)");Object action=send.invoke(channel,message);action.getClass().getMethod("queue").invoke(action);return true;});}
}
