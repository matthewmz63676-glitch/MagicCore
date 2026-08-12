package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.config.model.EventsFile;
import com.magicstudios.magiccore.modules.events.KothChanged;
import com.magicstudios.magiccore.modules.events.PinataChanged;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventPresentationController implements AutoCloseable {
    private static final String OWNER="event-presentation";private final Plugin plugin;private final SchedulerFacade scheduler;private final PlayerSettingsService settings;private final DomainEventBus events;private final List<EventsFile.ScheduledAnnouncement>announcements;private final MiniMessageRenderer renderer=new MiniMessageRenderer();private final List<TaskHandle>tasks=new CopyOnWriteArrayList<>();private final Set<java.util.UUID>announcedKoths=ConcurrentHashMap.newKeySet();private volatile boolean closed;
    public EventPresentationController(Plugin plugin,SchedulerFacade scheduler,PlayerSettingsService settings,DomainEventBus events,List<EventsFile.ScheduledAnnouncement>announcements){this.plugin=plugin;this.scheduler=scheduler;this.settings=settings;this.events=events;this.announcements=List.copyOf(announcements);}
    public void start(){events.subscribe(OWNER,KothChanged.class,this::koth);events.subscribe(OWNER,PinataChanged.class,this::pinata);for(var announcement:announcements)if(announcement.enabled())schedule(announcement,Duration.ofSeconds(announcement.firstDelaySeconds()));}
    private void koth(KothChanged event){if(event.status().equals("ACTIVE")){if(announcedKoths.add(event.runId()))announce("<gold><b>KOTH started</b></gold> <white>Capture "+safe(event.definitionId())+".</white>","BLOCK_BEACON_ACTIVATE","<gold>KOTH Started</gold>","<white>Hold the hill to win</white>");return;}if(event.status().equals("COMPLETED")){announcedKoths.remove(event.runId());announce("<green><b>KOTH captured</b></green> <white>"+safe(event.winner())+" won "+safe(event.definitionId())+".</white>","UI_TOAST_CHALLENGE_COMPLETE","<green>KOTH Captured</green>","<white>Winner: "+safe(event.winner())+"</white>");}else if(event.status().equals("CANCELLED"))announcedKoths.remove(event.runId());}
    private void pinata(PinataChanged event){if(event.status().equals("ACTIVE"))announce("<gold><b>Vote Party!</b></gold> <white>The pinata has spawned.</white>","ENTITY_FIREWORK_ROCKET_LAUNCH","<gold>Vote Party</gold>","<white>Find and hit the pinata</white>");else if(event.status().equals("COMPLETED"))announce("<green><b>Pinata complete!</b></green> <white>All "+event.maximumHits()+" hits were claimed.</white>","ENTITY_PLAYER_LEVELUP","<green>Pinata Complete</green>","<white>Thanks for voting</white>");}
    private void schedule(EventsFile.ScheduledAnnouncement value,Duration delay){tasks.add(scheduler.executeGlobalLater(delay,()->{if(closed)return;announce(value.message(),value.sound(),value.title(),value.subtitle());schedule(value,Duration.ofSeconds(value.intervalSeconds()));}));}
    private void announce(String message,String sound,String title,String subtitle){
        scheduler.executeGlobal(()->{
            Component rendered=renderer.render(message);
            for(Player player:plugin.getServer().getOnlinePlayers()){
                settings.get(player.getUniqueId()).thenAccept(preferences->{
                    if(!preferences.enabled(PlayerSetting.ANNOUNCEMENTS))return;
                    scheduler.executeEntity(player,()->{
                        player.sendMessage(rendered);
                        if(preferences.enabled(PlayerSetting.SOUNDS)&&sound!=null&&!sound.isBlank()){Sound selected=Registry.SOUNDS.get(NamespacedKey.minecraft(sound.toLowerCase(java.util.Locale.ROOT)));if(selected!=null)player.playSound(player.getLocation(),selected,1.0f,1.0f);}
                        if(preferences.enabled(PlayerSetting.PARTICLES))player.spawnParticle(Particle.HAPPY_VILLAGER,player.getLocation().add(0,1,0),8,0.4,0.4,0.4,0.01);
                        if(title!=null&&!title.isBlank())player.showTitle(Title.title(renderer.render(title),renderer.render(subtitle==null?"":subtitle)));
                    },()->{});
                });
            }
        });
    }
    @Override public void close(){closed=true;events.unsubscribeOwner(OWNER);tasks.forEach(TaskHandle::cancel);tasks.clear();announcedKoths.clear();}
    private static String safe(String value){return value==null?"":value.replace('<','‹').replace('>','›');}
}
