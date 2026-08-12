package com.magicstudios.magiccore.integrations.voting;

import com.magicstudios.magiccore.bootstrap.PinataController;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Reflection keeps NuVotifier optional while accepting only its trusted Bukkit event. */
public final class NuVotifierVoteBridge implements Listener,AutoCloseable {
    private final Plugin plugin;private final PlayerProfileService profiles;private final PinataController pinata;private final Clock clock;
    private NuVotifierVoteBridge(Plugin plugin,PlayerProfileService profiles,PinataController pinata,Clock clock){this.plugin=plugin;this.profiles=profiles;this.pinata=pinata;this.clock=clock;}
    public static NuVotifierVoteBridge register(Plugin plugin,PlayerProfileService profiles,PinataController pinata,Clock clock){try{Class<?>raw=Class.forName("com.vexsoftware.votifier.model.VotifierEvent",false,plugin.getServer().getPluginManager().getPlugin("Votifier")==null?plugin.getClass().getClassLoader():plugin.getServer().getPluginManager().getPlugin("Votifier").getClass().getClassLoader());if(!Event.class.isAssignableFrom(raw))throw new IllegalStateException("VotifierEvent is not a Bukkit event");NuVotifierVoteBridge bridge=new NuVotifierVoteBridge(plugin,profiles,pinata,clock);@SuppressWarnings("unchecked")Class<? extends Event>eventType=(Class<? extends Event>)raw;plugin.getServer().getPluginManager().registerEvent(eventType,bridge,EventPriority.MONITOR,(listener,event)->bridge.receive(event),plugin,true);return bridge;}catch(ClassNotFoundException unavailable){return new NuVotifierVoteBridge(plugin,profiles,pinata,clock);}}
    private void receive(Event event){try{Object vote=event.getClass().getMethod("getVote").invoke(event);String username=text(vote,"getUsername");String service=text(vote,"getServiceName");String timestamp=text(vote,"getTimeStamp");String providerId=service+":"+username+":"+timestamp;Player online=plugin.getServer().getPlayerExact(username);if(online!=null){pinata.acceptVerifiedVote(providerId,online.getUniqueId(),service,parseTime(timestamp),true);return;}profiles.findByCurrentName(username).thenAccept(profile->{if(profile.isEmpty()){plugin.getLogger().warning("Ignored verified offline vote for unknown profile "+username);return;}pinata.acceptVerifiedVote(providerId,profile.get().playerId(),service,parseTime(timestamp),false);});}catch(ReflectiveOperationException failure){plugin.getLogger().warning("Could not normalize NuVotifier event: "+failure.getMessage());}}
    private Instant parseTime(String value){try{long epoch=Long.parseLong(value);return Instant.ofEpochSecond(epoch);}catch(RuntimeException ignored){return clock.instant();}}
    private static String text(Object target,String method)throws ReflectiveOperationException{Method value=target.getClass().getMethod(method);Object result=value.invoke(target);return result==null?"":result.toString();}
    @Override public void close(){HandlerList.unregisterAll(this);}
}
