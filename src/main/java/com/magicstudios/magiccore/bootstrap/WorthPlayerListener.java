package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.worth.ItemValuationService;
import com.magicstudios.magiccore.placeholders.WorthPlaceholderView;
import com.magicstudios.magiccore.platform.BukkitValuationInputs;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WorthPlayerListener implements Listener {
    private final SchedulerFacade scheduler;private final ItemValuationService valuation;private final WorthPlaceholderView view;
    public WorthPlayerListener(SchedulerFacade scheduler,ItemValuationService valuation,WorthPlaceholderView view){this.scheduler=scheduler;this.valuation=valuation;this.view=view;}
    @EventHandler(priority=EventPriority.MONITOR)public void onJoin(PlayerJoinEvent event){refresh(event.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void onHeld(PlayerItemHeldEvent event){scheduler.executeEntity(event.getPlayer(),()->update(event.getPlayer(),event.getPlayer().getInventory().getItem(event.getNewSlot())),()->{});}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void onInventory(InventoryClickEvent event){if(event.getWhoClicked() instanceof Player player)refresh(player);}
    @EventHandler public void onQuit(PlayerQuitEvent event){view.remove(event.getPlayer().getUniqueId());}
    public void refresh(Player player){scheduler.executeEntity(player,()->update(player,player.getInventory().getItemInMainHand()),()->{});}
    private void update(Player player,org.bukkit.inventory.ItemStack item){if(item==null||item.getType().isAir()){view.unavailable(player.getUniqueId());return;}view.update(player.getUniqueId(),valuation.value(BukkitValuationInputs.from(item)));}
}
