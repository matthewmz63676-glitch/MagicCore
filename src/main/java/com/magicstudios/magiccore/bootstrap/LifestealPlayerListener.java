package com.magicstudios.magiccore.bootstrap;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.lifesteal.HeartAccount;
import com.magicstudios.magiccore.modules.lifesteal.HeartTransferred;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.lifesteal.PlayerEliminated;
import com.magicstudios.magiccore.modules.lifesteal.PlayerRevived;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public final class LifestealPlayerListener implements Listener,AutoCloseable{
 private final Plugin plugin;private final SchedulerFacade scheduler;private final LifestealService service;private final DomainEventBus events;
 private final String owner,eliminationAction;private final boolean revivalEnabled;private final NamespacedKey marker;
 public LifestealPlayerListener(Plugin plugin,SchedulerFacade scheduler,LifestealService service,DomainEventBus events,String eliminationAction,boolean revivalEnabled){
  this.plugin=plugin;this.scheduler=scheduler;this.service=service;this.events=events;this.eliminationAction=eliminationAction;this.owner="lifesteal-platform";this.marker=new NamespacedKey(plugin,"heart_item");
  this.revivalEnabled=revivalEnabled;
  events.subscribe(owner,HeartTransferred.class,event->{refresh(event.killerId());refresh(event.victimId());});
  events.subscribe(owner,PlayerEliminated.class,event->eliminate(event.playerId()));
  events.subscribe(owner,PlayerRevived.class,event->revive(event.playerId()));
 }
 @EventHandler public void onJoin(PlayerJoinEvent event){UUID id=event.getPlayer().getUniqueId();service.account(id).whenComplete((account,failure)->{if(failure==null)scheduler.executeEntity(event.getPlayer(),()->apply(event.getPlayer(),account),()->{});});}
 @EventHandler public void onUse(PlayerInteractEvent event){if(event.getAction()!=Action.RIGHT_CLICK_AIR&&event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;ItemStack held=event.getItem();
  if(held==null)return;String kind=held.getPersistentDataContainer().get(marker,PersistentDataType.STRING);if(!"HEART".equals(kind)&&!"REVIVAL".equals(kind))return;event.setCancelled(true);Player player=event.getPlayer();
  if("REVIVAL".equals(kind)&&!revivalEnabled){player.sendMessage(Component.text("Revival items are disabled."));return;}
  ItemStack recovery=held.clone();recovery.setAmount(1);held.setAmount(held.getAmount()-1);String operation="heart-item:"+UUID.randomUUID();
  var mutation="REVIVAL".equals(kind)?service.revive(player.getUniqueId(),operation):service.consume(player.getUniqueId(),operation);
  mutation.whenComplete((result,failure)->{if(failure!=null)scheduler.executeEntity(player,()->{
      var leftovers=player.getInventory().addItem(recovery);if(!leftovers.isEmpty())plugin.getLogger().severe("Could not restore failed heart consumption "+operation);player.sendMessage(Component.text("Heart use failed: "+root(failure)));},()->{});
    else scheduler.executeEntity(player,()->apply(player,result.player()),()->{});});}
 private void refresh(UUID id){scheduler.executeGlobal(()->{Player player=plugin.getServer().getPlayer(id);if(player!=null)service.account(id).whenComplete((account,failure)->{if(failure==null)scheduler.executeEntity(player,()->apply(player,account),()->{});});});}
 private void eliminate(UUID id){scheduler.executeGlobal(()->{Player player=plugin.getServer().getPlayer(id);if(player==null)return;scheduler.executeEntity(player,()->{if(eliminationAction.equals("KICK"))player.kick(Component.text("You have been eliminated."));else player.setGameMode(GameMode.SPECTATOR);},()->{});});}
 private void revive(UUID id){scheduler.executeGlobal(()->{Player player=plugin.getServer().getPlayer(id);if(player==null)return;service.account(id).whenComplete((account,failure)->{if(failure==null)scheduler.executeEntity(player,()->{player.setGameMode(GameMode.SURVIVAL);apply(player,account);},()->{});});});}
 private void apply(Player player,HeartAccount account){var attribute=player.getAttribute(Attribute.MAX_HEALTH);if(attribute!=null)attribute.setBaseValue(Math.max(2,account.hearts()*2.0));if(player.getHealth()>account.hearts()*2.0)player.setHealth(account.hearts()*2.0);if(account.eliminated()){if(eliminationAction.equals("KICK"))player.kick(Component.text("You have been eliminated."));else player.setGameMode(GameMode.SPECTATOR);}}
 private static String root(Throwable failure){Throwable current=failure;while(current.getCause()!=null)current=current.getCause();return current.getMessage();}
 @Override public void close(){events.unsubscribeOwner(owner);}
}
