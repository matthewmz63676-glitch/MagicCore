package com.magicstudios.magiccore.integrations.vulcan;

import com.magicstudios.magiccore.api.DomainEventBus;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VulcanIntegrationService implements VulcanService {
    private final boolean available;private final String status;private final VulcanFlagBuffer buffer;private final Listener listener;
    private VulcanIntegrationService(boolean available,String status,VulcanFlagBuffer buffer,Listener listener){this.available=available;this.status=status;this.buffer=buffer;this.listener=listener;}
    public static VulcanService create(Plugin plugin,DomainEventBus events,Clock clock,VulcanFlagBuffer buffer,boolean enabled,boolean captureFlags){
        if(!enabled)return unavailable("DISABLED",buffer);if(plugin.getServer().getPluginManager().getPlugin("Vulcan")==null)return unavailable("VULCAN_NOT_INSTALLED",buffer);if(!captureFlags)return new VulcanIntegrationService(true,"DETECTED_FLAG_CAPTURE_DISABLED",buffer,null);
        Class<? extends Event>eventType=findEventType();if(eventType==null)return unavailable("VULCAN_FLAG_EVENT_API_UNAVAILABLE",buffer);Listener listener=new Listener(){};EventExecutor executor=(ignored,event)->observe(event,buffer,events,clock);
        plugin.getServer().getPluginManager().registerEvent(eventType,listener,EventPriority.MONITOR,executor,plugin,true);return new VulcanIntegrationService(true,"FLAG_CAPTURE_ACTIVE",buffer,listener);
    }
    private static VulcanService unavailable(String status,VulcanFlagBuffer buffer){return new VulcanIntegrationService(false,status,buffer,null);}
    @SuppressWarnings("unchecked")private static Class<? extends Event>findEventType(){for(String name:List.of("me.frep.vulcan.api.event.VulcanFlagEvent","me.frep.vulcan.api.events.VulcanFlagEvent","me.frep.vulcan.spigot.event.VulcanFlagEvent"))try{Class<?>type=Class.forName(name);if(Event.class.isAssignableFrom(type))return(Class<? extends Event>)type;}catch(ClassNotFoundException ignored){}return null;}
    private static void observe(Event event,VulcanFlagBuffer buffer,DomainEventBus events,Clock clock){try{Object playerValue=invokeFirst(event,List.of("getPlayer"));if(!(playerValue instanceof Player player))return;Object checkValue=invokeFirst(event,List.of("getCheck","getCheckName","getType"));String check=stringValue(checkValue);Object violation=invokeFirstOrNull(event,List.of("getVl","getViolationLevel","getViolations"));double level=violation instanceof Number number?number.doubleValue():0D;Object detail=invokeFirstOrNull(event,List.of("getInfo","getData","getMessage"));Instant now=clock.instant();VulcanFlag flag=new VulcanFlag(player.getUniqueId(),check,level,detail==null?"":detail.toString(),now);buffer.record(flag);events.publish(new VulcanFlagObserved(player.getUniqueId(),flag,now));}catch(ReflectiveOperationException ignored){/* Provider API drift is exposed by health/status on next restart; never alter the flag. */}}
    private static Object invokeFirst(Object target,List<String>names)throws ReflectiveOperationException{Object result=invokeFirstOrNull(target,names);if(result==null)throw new NoSuchMethodException(target.getClass().getName()+" "+names);return result;}
    private static Object invokeFirstOrNull(Object target,List<String>names)throws ReflectiveOperationException{for(String name:names)try{return target.getClass().getMethod(name).invoke(target);}catch(NoSuchMethodException ignored){}return null;}
    private static String stringValue(Object value){if(value==null)return "UNKNOWN";if(value instanceof String string)return string;for(String method:List.of("getName","getType","name"))try{Object nested=value.getClass().getMethod(method).invoke(value);if(nested!=null)return nested.toString();}catch(ReflectiveOperationException ignored){}return value.toString();}
    @Override public boolean available(){return available;}@Override public String status(){return status;}@Override public List<VulcanFlag>recentFlags(UUID playerId,Instant after){return buffer.recent(playerId,after);}@Override public void close(){if(listener!=null)HandlerList.unregisterAll(listener);}
}
