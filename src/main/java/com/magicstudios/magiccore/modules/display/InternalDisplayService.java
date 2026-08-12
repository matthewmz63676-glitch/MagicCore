package com.magicstudios.magiccore.modules.display;

import com.magicstudios.magiccore.config.model.DisplayFile;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.placeholders.PlaceholderContext;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import com.magicstudios.magiccore.ranks.RankDefinition;
import com.magicstudios.magiccore.ranks.RankService;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InternalDisplayService implements DisplayService, Listener, AutoCloseable {
    private static final Pattern PLACEHOLDER=Pattern.compile("%magiccore_([a-z][a-z0-9_]*)%");
    private final Plugin plugin;private final SchedulerFacade scheduler;private final PlayerSettingsService settings;private final RankService ranks;
    private final PlaceholderRegistry placeholders;private final DisplayFile config;private final Map<String,RankDefinition>rankDefinitions;
    private final MiniMessageRenderer text=new MiniMessageRenderer();private final Map<UUID,Identity>identities=new ConcurrentHashMap<>();
    private final AtomicBoolean closed=new AtomicBoolean();private final AtomicReference<TaskHandle>nextRefresh=new AtomicReference<>();

    public InternalDisplayService(Plugin plugin,SchedulerFacade scheduler,PlayerSettingsService settings,RankService ranks,
                                  PlaceholderRegistry placeholders,DisplayFile config,Map<String,RankDefinition>rankDefinitions){
        this.plugin=plugin;this.scheduler=scheduler;this.settings=settings;this.ranks=ranks;this.placeholders=placeholders;this.config=config;this.rankDefinitions=Map.copyOf(rankDefinitions);scheduleNext();}
    @Override public String provider(){return "INTERNAL";}
    @Override public void refresh(UUID playerId){scheduler.executeGlobal(()->{Player player=plugin.getServer().getPlayer(playerId);if(player==null)return;
        scheduler.executeEntity(player,()->loadAndRender(player),()->identities.remove(playerId));});}
    private void loadAndRender(Player player){UUID id=player.getUniqueId();String name=player.getName();var rankFuture=ranks.rankOf(id).toCompletableFuture();var settingsFuture=settings.get(id).toCompletableFuture();
        java.util.concurrent.CompletableFuture.allOf(rankFuture,settingsFuture).whenComplete((ignored,failure)->{if(failure!=null)return;String rankId=rankFuture.join();RankDefinition rank=rankDefinitions.get(rankId);
            identities.put(id,new Identity(name,rankId,rank==null?0:rank.weight(),rank==null?rankId:rank.display()));
            scheduler.executeGlobal(()->{Scoreboard board=createBoard(id,settingsFuture.join().enabled(PlayerSetting.SCOREBOARD));scheduler.executeEntity(player,()->apply(player,board,rankId,name),()->identities.remove(id));});});}
    @Override public void refreshAll(){scheduler.executeGlobal(()->plugin.getServer().getOnlinePlayers().forEach(player->refresh(player.getUniqueId())));}
    @EventHandler public void onJoin(PlayerJoinEvent event){scheduler.executeGlobalLater(Duration.ofSeconds(1),()->refresh(event.getPlayer().getUniqueId()));}
    @EventHandler public void onQuit(PlayerQuitEvent event){identities.remove(event.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)public void onChat(AsyncChatEvent event){if(!config.chat().enabled())return;Identity identity=identities.get(event.getPlayer().getUniqueId());if(identity==null)return;
        String before=config.chat().format();int marker=before.indexOf("<message>");String prefix=marker<0?before:before.substring(0,marker);String suffix=marker<0?"":before.substring(marker+9);
        Component prefixComponent=text.render(resolveTemplate(prefix,event.getPlayer().getUniqueId()).replace("%player_name%",identity.name()).replace("%magiccore_ranks_id%",identity.rankId()));
        Component suffixComponent=text.render(resolveTemplate(suffix,event.getPlayer().getUniqueId()).replace("%player_name%",identity.name()).replace("%magiccore_ranks_id%",identity.rankId()));
        event.renderer((source,sourceDisplayName,message,viewer)->prefixComponent.append(message).append(suffixComponent));
        if(config.chat().mentionsEnabled()){String plain=PlainTextComponentSerializer.plainText().serialize(event.message()).toLowerCase();identities.forEach((id,target)->{if(!id.equals(event.getPlayer().getUniqueId())&&plain.contains(target.name().toLowerCase()))
            scheduler.executeGlobal(()->{Player mentioned=plugin.getServer().getPlayer(id);if(mentioned!=null)scheduler.executeEntity(mentioned,()->mentioned.playSound(mentioned.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,1f,1.2f),()->{});});});}}

    private Scoreboard createBoard(UUID viewer,boolean scoreboardEnabled){Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        if(scoreboardEnabled&&config.scoreboard().enabled()){Objective objective=board.registerNewObjective("magic_sidebar",Criteria.DUMMY,text.render(resolveTemplate(config.scoreboard().title(),viewer)));objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            int score=config.scoreboard().lines().size();int index=0;for(String line:config.scoreboard().lines()){String legacy=LegacyComponentSerializer.legacySection().serialize(text.render(resolveTemplate(line,viewer)))+ChatColor.values()[index++%16];objective.getScore(legacy).setScore(score--);}}
        if(config.belowName().enabled()){Objective health=board.registerNewObjective("magic_health",Criteria.HEALTH,text.render(config.belowName().label()));health.setDisplaySlot(DisplaySlot.BELOW_NAME);}
        identities.forEach((id,identity)->{String teamId=String.format("r%05d",Math.max(0,99999-Math.max(0,identity.weight())));Team team=board.getTeam(teamId);if(team==null)team=board.registerNewTeam(teamId);team.prefix(text.render(identity.display()+" "));team.addEntry(identity.name());});return board;}
    private void apply(Player player,Scoreboard board,String rankId,String name){player.setScoreboard(board);if(config.tab().enabled()){
        player.sendPlayerListHeaderAndFooter(text.render(resolveTemplate(config.tab().header(),player.getUniqueId())),text.render(resolveTemplate(config.tab().footer(),player.getUniqueId())));
        player.playerListName(text.render(resolveTemplate(config.tab().nameFormat(),player.getUniqueId()).replace("%player_name%",name).replace("%magiccore_ranks_id%",rankId)));}}
    private String resolveTemplate(String template,UUID subject){Matcher matcher=PLACEHOLDER.matcher(template);StringBuffer result=new StringBuffer();while(matcher.find())matcher.appendReplacement(result,Matcher.quoteReplacement(placeholders.resolve(matcher.group(1),new PlaceholderContext(subject,subject))));matcher.appendTail(result);return result.toString();}
    private void scheduleNext(){if(closed.get())return;nextRefresh.set(scheduler.executeGlobalLater(Duration.ofSeconds(config.refreshSeconds()),()->{if(!closed.get()){refreshAll();scheduleNext();}}));}
    @Override public void close(){closed.set(true);TaskHandle task=nextRefresh.getAndSet(null);if(task!=null)task.cancel();identities.clear();}
    private record Identity(String name,String rankId,int weight,String display){}
}
