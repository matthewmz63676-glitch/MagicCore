package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.config.model.EventsFile;
import com.magicstudios.magiccore.modules.events.PinataParty;
import com.magicstudios.magiccore.modules.events.VotePartyOutcome;
import com.magicstudios.magiccore.modules.events.VotePartyService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class PinataController implements Listener,AutoCloseable {
    private final Plugin plugin;private final SchedulerFacade scheduler;private final VotePartyService service;private final PlayerSettingsService settings;private final EventsFile.VoteParty policy;private final Clock clock;private final NamespacedKey partyKey;private final MiniMessageRenderer renderer=new MiniMessageRenderer();private volatile Entity entity;private volatile BossBar bossbar;private volatile UUID activeParty;private volatile boolean closed;
    public PinataController(Plugin plugin,SchedulerFacade scheduler,VotePartyService service,PlayerSettingsService settings,EventsFile.VoteParty policy,Clock clock){this.plugin=plugin;this.scheduler=scheduler;this.service=service;this.settings=settings;this.policy=policy;this.clock=clock;this.partyKey=new NamespacedKey(plugin,"pinata_party");}
    public void start(){service.activeParty().thenAccept(active->{if(active.isPresent())spawn(active.get());else activateNext();});}
    public java.util.concurrent.CompletionStage<VotePartyOutcome>acceptVerifiedVote(String providerEventId,UUID playerId,String voteService,Instant occurredAt,boolean online){return service.recordVerifiedVote(providerEventId,playerId,voteService,occurredAt,online).thenApply(outcome->{if(outcome.triggeredParty().isPresent())activateNext();return outcome;});}
    public void activateNext(){if(closed||activeParty!=null)return;service.activeParty().thenCompose(active->{if(active.isPresent())return CompletableFuture.completedFuture(active.get());return service.pendingParties(1).thenCompose(pending->pending.isEmpty()?CompletableFuture.completedFuture(null):service.activate(pending.getFirst().id(),"pinata-auto-activate:"+pending.getFirst().id()));}).whenComplete((party,failure)->{if(failure!=null)plugin.getLogger().warning("Could not activate vote pinata: "+root(failure));else if(party!=null)spawn(party);});}
    private void spawn(PinataParty party){if(closed||activeParty!=null)return;activeParty=party.id();scheduler.executeGlobal(()->{World world=plugin.getServer().getWorld(policy.pinata().world());if(world==null){plugin.getLogger().severe("Pinata world is unavailable: "+policy.pinata().world());service.cancel(party.id(),"pinata-world-unavailable:"+party.id());activeParty=null;return;}Location location=new Location(world,policy.pinata().x(),policy.pinata().y(),policy.pinata().z());scheduler.executeRegion(location,()->{if(closed)return;EntityType type=EntityType.valueOf(policy.pinata().entityType());Entity spawned=world.spawnEntity(location,type);spawned.getPersistentDataContainer().set(partyKey,PersistentDataType.STRING,party.id().toString());spawned.setPersistent(true);spawned.setGlowing(true);if(spawned instanceof LivingEntity living){living.setAI(false);living.setRemoveWhenFarAway(false);}entity=spawned;createBossbar(party);});});}
    private void createBossbar(PinataParty party){bossbar=BossBar.bossBar(title(party),progress(party),BossBar.Color.YELLOW,BossBar.Overlay.PROGRESS);for(Player player:plugin.getServer().getOnlinePlayers())settings.get(player.getUniqueId()).thenAccept(value->{if(value.enabled(PlayerSetting.BOSSBAR))scheduler.executeEntity(player,()->player.showBossBar(bossbar),()->{});});}
    @EventHandler(ignoreCancelled=true)public void onHit(EntityDamageByEntityEvent event){String value=event.getEntity().getPersistentDataContainer().get(partyKey,PersistentDataType.STRING);if(value==null)return;event.setCancelled(true);if(!(event.getDamager()instanceof Player player))return;UUID partyId;try{partyId=UUID.fromString(value);}catch(IllegalArgumentException invalid){return;}service.hit(partyId,player.getUniqueId(),"pinata-hit:"+partyId+":"+player.getUniqueId()+":"+UUID.randomUUID()).whenComplete((party,failure)->{if(failure!=null){scheduler.executeEntity(player,()->player.sendMessage(net.kyori.adventure.text.Component.text("Pinata hit rejected: "+root(failure))),()->{});return;}update(party);});}
    private void update(PinataParty party){BossBar bar=bossbar;if(bar!=null){bar.name(title(party));bar.progress(progress(party));}if(party.status()==PinataParty.Status.COMPLETED||party.status()==PinataParty.Status.CANCELLED){Entity current=entity;if(current!=null)scheduler.executeEntity(current,current::remove,()->{});entity=null;activeParty=null;BossBar old=bossbar;bossbar=null;if(old!=null)for(Player player:plugin.getServer().getOnlinePlayers())scheduler.executeEntity(player,()->player.hideBossBar(old),()->{});activateNext();}}
    private net.kyori.adventure.text.Component title(PinataParty party){return renderer.render(policy.pinata().bossbarTitle().replace("{remaining}",Integer.toString(party.remaining())).replace("{maximum}",Integer.toString(party.maximumHits())));}
    private static float progress(PinataParty party){return Math.max(0.0f,Math.min(1.0f,(float)party.remaining()/party.maximumHits()));}
    @Override public void close(){closed=true;HandlerList.unregisterAll(this);Entity current=entity;if(current!=null)scheduler.executeEntity(current,current::remove,()->{});BossBar old=bossbar;if(old!=null)for(Player player:plugin.getServer().getOnlinePlayers())scheduler.executeEntity(player,()->player.hideBossBar(old),()->{});entity=null;bossbar=null;activeParty=null;}
    private static String root(Throwable failure){Throwable value=failure;while(value.getCause()!=null)value=value.getCause();return value.getMessage();}
}
