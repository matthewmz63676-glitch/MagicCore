package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.placeholders.AfkPlaceholderView;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AfkShardController implements Listener,AutoCloseable {
    private final Plugin plugin;private final SchedulerFacade scheduler;private final ShardService shards;private final AfkPlaceholderView placeholders;private final AfkZoneMatcher zones;private final AfkPresenceTracker tracker=new AfkPresenceTracker();private final Clock clock;private final Duration interval;private final AtomicBoolean closed=new AtomicBoolean();private final AtomicReference<TaskHandle>next=new AtomicReference<>();
    public AfkShardController(Plugin plugin,SchedulerFacade scheduler,ShardService shards,AfkPlaceholderView placeholders,AfkZoneMatcher zones,Clock clock,Duration interval){this.plugin=plugin;this.scheduler=scheduler;this.shards=shards;this.placeholders=placeholders;this.zones=zones;this.clock=clock;this.interval=interval;schedule();}
    @EventHandler public void onJoin(PlayerJoinEvent event){placeholders.refresh(event.getPlayer().getUniqueId());}
    @EventHandler public void onQuit(PlayerQuitEvent event){tracker.remove(event.getPlayer().getUniqueId());}
    private void schedule(){if(closed.get())return;next.set(scheduler.executeGlobalLater(interval,this::tick));}
    private void tick(){if(closed.get())return;for(Player player:new ArrayList<>(plugin.getServer().getOnlinePlayers()))scheduler.executeEntity(player,()->sample(player),()->tracker.remove(player.getUniqueId()));schedule();}
    private void sample(Player player){var zone=zones.match(player.getLocation());if(zone.isEmpty()){tracker.outside(player.getUniqueId());return;}var now=clock.instant();var eligibility=tracker.sample(player.getUniqueId(),zone.get(),player.getLocation(),now);long bucket=Math.floorDiv(now.getEpochSecond(),Math.max(1,interval.toSeconds()));shards.award(player.getUniqueId(),eligibility,zone.get()+":"+bucket).exceptionally(failure->{plugin.getLogger().warning("AFK shard award failed for "+player.getUniqueId()+": "+failure.getMessage());return null;});}
    @Override public void close(){if(!closed.compareAndSet(false,true))return;TaskHandle handle=next.getAndSet(null);if(handle!=null)handle.cancel();HandlerList.unregisterAll(this);}
}
