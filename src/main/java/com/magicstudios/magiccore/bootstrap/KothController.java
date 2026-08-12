package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.events.*;
import com.magicstudios.magiccore.modules.teams.Team;
import com.magicstudios.magiccore.modules.teams.TeamService;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Folia-safe platform coordinator; all player snapshots are taken on entity schedulers. */
public final class KothController implements Listener,AutoCloseable {
    private final Plugin plugin;private final SchedulerFacade scheduler;private final KothService service;private final TeamService teams;private final Clock clock;private final Duration tick;private final List<TaskHandle>tasks=new CopyOnWriteArrayList<>();private final Set<String>activeDefinitions=ConcurrentHashMap.newKeySet();private volatile boolean closed;
    public KothController(Plugin plugin,SchedulerFacade scheduler,KothService service,TeamService teams,Clock clock,Duration tick){this.plugin=plugin;this.scheduler=scheduler;this.service=service;this.teams=teams;this.clock=clock;this.tick=tick;}
    public void start(){for(KothDefinition definition:service.definitions())scheduleStart(definition,definition.firstDelay());scheduleTick(tick);}
    private void scheduleStart(KothDefinition definition,Duration delay){tasks.add(scheduler.executeGlobalLater(delay,()->{if(closed)return;service.active(definition.id()).thenCompose(active->active.isPresent()?CompletableFuture.completedFuture(active.get()):service.start(definition.id(),"scheduled:"+definition.id()+":"+clock.instant().toEpochMilli())).whenComplete((run,failure)->{if(failure!=null)plugin.getLogger().warning("Could not start KOTH "+definition.id()+": "+root(failure));});scheduleStart(definition,definition.scheduleInterval());}));}
    private void scheduleTick(Duration delay){tasks.add(scheduler.executeGlobalLater(delay,()->{if(closed)return;tickAll();scheduleTick(tick);}));}
    private void tickAll(){for(KothDefinition definition:service.definitions())service.active(definition.id()).whenComplete((active,failure)->{if(failure!=null){plugin.getLogger().warning("Could not inspect KOTH "+definition.id()+": "+root(failure));return;}if(active.isEmpty()){activeDefinitions.remove(definition.id());return;}activeDefinitions.add(definition.id());sample(definition,active.get());});}
    private void sample(KothDefinition definition,KothRun run){List<Player>online=new ArrayList<>(plugin.getServer().getOnlinePlayers());if(online.isEmpty()){service.sample(run.id(),List.of(),tick,"koth-tick:"+run.id()+":"+clock.instant().toEpochMilli());return;}List<PlayerSnapshot>snapshots=Collections.synchronizedList(new ArrayList<>());AtomicInteger remaining=new AtomicInteger(online.size());for(Player player:online)scheduler.executeEntity(player,()->{Location location=player.getLocation();if(definition.contains(location.getWorld().getName(),location.getX(),location.getY(),location.getZ())&&!carriesBanned(player,definition))snapshots.add(new PlayerSnapshot(player.getUniqueId()));if(remaining.decrementAndGet()==0)resolve(run,snapshots);},()->{if(remaining.decrementAndGet()==0)resolve(run,snapshots);});}
    private void resolve(KothRun run,List<PlayerSnapshot>snapshots){List<CompletableFuture<Optional<Team>>>lookups=snapshots.stream().map(value->teams.teamOf(value.playerId()).toCompletableFuture()).toList();CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new)).thenCompose(ignored->{Map<String,Builder>groups=new LinkedHashMap<>();for(int index=0;index<snapshots.size();index++){UUID player=snapshots.get(index).playerId();Optional<Team>team=lookups.get(index).join();if(team.isPresent()){Team value=team.get();groups.computeIfAbsent("team:"+value.id(),key->new Builder(value.name())).recipients.addAll(value.members().keySet());}else groups.computeIfAbsent("player:"+player,key->new Builder("Solo player")).recipients.add(player);}List<KothContender>contenders=groups.entrySet().stream().map(entry->new KothContender(entry.getKey(),entry.getValue().name,entry.getValue().recipients)).toList();return service.sample(run.id(),contenders,tick,"koth-tick:"+run.id()+":"+clock.instant().toEpochMilli());}).whenComplete((updated,failure)->{if(failure!=null)plugin.getLogger().warning("Could not advance KOTH "+run.id()+": "+root(failure));else if(updated.status()!=KothRun.Status.ACTIVE)activeDefinitions.remove(updated.definitionId());});}
    @EventHandler(ignoreCancelled=true)public void onUse(PlayerInteractEvent event){ItemStack item=event.getItem();if(item==null)return;Location location=event.getPlayer().getLocation();for(KothDefinition definition:service.definitions())if(activeDefinitions.contains(definition.id())&&definition.contains(location.getWorld().getName(),location.getX(),location.getY(),location.getZ())&&definition.bannedMaterials().contains(item.getType().name())){event.setCancelled(true);event.getPlayer().sendMessage(Component.text("That item is restricted while contesting "+definition.displayName()+"."));return;}}
    private static boolean carriesBanned(Player player,KothDefinition definition){for(ItemStack item:player.getInventory().getContents())if(item!=null&&definition.bannedMaterials().contains(item.getType().name()))return true;return false;}
    @Override public void close(){closed=true;tasks.forEach(TaskHandle::cancel);tasks.clear();activeDefinitions.clear();HandlerList.unregisterAll(this);}
    private static String root(Throwable failure){Throwable value=failure;while(value.getCause()!=null)value=value.getCause();return value.getMessage();}
    private record PlayerSnapshot(UUID playerId){}
    private static final class Builder{private final String name;private final Set<UUID>recipients=new LinkedHashSet<>();private Builder(String name){this.name=name;}}
}
