package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public final class MagicGuiController implements Listener,AutoCloseable {
 private final SchedulerFacade scheduler;private final MenusFile.Theme theme;private final GuiItemFactory items=new GuiItemFactory();private final MiniMessageRenderer renderer=new MiniMessageRenderer();private final Map<UUID,Session>sessions=new ConcurrentHashMap<>();private final Map<String,GuiMenu>menus=new ConcurrentHashMap<>();
 public MagicGuiController(SchedulerFacade scheduler,MenusFile.Theme theme){this.scheduler=scheduler;this.theme=theme;}
 public void register(GuiMenu menu){if(menus.putIfAbsent(menu.id(),menu)!=null)throw new IllegalStateException("Duplicate GUI menu "+menu.id());}
 public boolean hasMenu(String menuId){return menus.containsKey(menuId);}
 public java.util.Set<String>menuIds(){return java.util.Set.copyOf(menus.keySet());}
 public void open(Player player,String menuId){GuiMenu menu=menus.get(menuId);if(menu==null){player.sendMessage(Component.text("Menu is unavailable: "+menuId));return;}open(player,menu,0);}
 public void open(Player player,GuiMenu menu,int page){CompletionStage<GuiPage>rendered;try{rendered=menu.render(player,page);}catch(Throwable failure){player.sendMessage(Component.text("Could not render menu: "+failure.getMessage()));return;}rendered.whenComplete((view,failure)->{if(failure!=null){scheduler.executeEntity(player,()->player.sendMessage(Component.text("Could not render menu: "+root(failure).getMessage())),()->{});return;}scheduler.executeEntity(player,()->show(player,menu,view),()->{});});}
 public void refresh(Player player){Session session=sessions.get(player.getUniqueId());if(session!=null)open(player,session.menu(),session.page().page());}
 public void close(Player player){scheduler.executeEntity(player,player::closeInventory,()->{});}
 private void show(Player player,GuiMenu menu,GuiPage page){UUID sessionId=UUID.randomUUID();Holder holder=new Holder(sessionId,menu.id());Inventory inventory=Bukkit.createInventory(holder,page.rows()*9,items.component(page.title()));holder.inventory=inventory;for(int slot=0;slot<inventory.getSize();slot++)inventory.setItem(slot,items.blank(theme.fillMaterial()));page.elements().forEach((slot,element)->{if(slot>=0&&slot<inventory.getSize())inventory.setItem(slot,element.item().clone());});if(page.page()>0)inventory.setItem(inventory.getSize()-9,items.item(theme.previousMaterial(),"<aqua>◄ Previous Page",java.util.List.of("<gray>Return to page "+page.page())));inventory.setItem(inventory.getSize()-5,items.item(theme.closeMaterial(),"<red>✕ Close",java.util.List.of()));if(page.page()+1<page.pageCount())inventory.setItem(inventory.getSize()-1,items.item(theme.nextMaterial(),"<aqua>Next Page ►",java.util.List.of("<gray>Open page "+(page.page()+2))));Session session=new Session(sessionId,menu,page);sessions.put(player.getUniqueId(),session);player.openInventory(inventory);}
 @EventHandler(priority=EventPriority.HIGHEST)public void onClick(InventoryClickEvent event){boolean magic=event.getView().getTopInventory().getHolder(false)instanceof Holder;if(GuiSlotPolicy.cancelClick(magic))event.setCancelled(true);if(!magic)return;Holder holder=(Holder)event.getView().getTopInventory().getHolder(false);if(!(event.getWhoClicked()instanceof Player player))return;Session session=sessions.get(player.getUniqueId());int size=event.getView().getTopInventory().getSize();if(session==null||!session.id().equals(holder.sessionId)||!GuiSlotPolicy.actionableTopSlot(true,event.getRawSlot(),size))return;int slot=event.getRawSlot();if(slot==GuiSlotPolicy.closeSlot(size)){player.closeInventory();return;}if(slot==GuiSlotPolicy.previousOrBackSlot(size)&&session.page().page()>0){open(player,session.menu(),session.page().page()-1);return;}if(slot==GuiSlotPolicy.nextSlot(size)&&session.page().page()+1<session.page().pageCount()){open(player,session.menu(),session.page().page()+1);return;}GuiElement element=session.page().elements().get(slot);if(element==null||element.action()==null||element.action().isBlank())return;try{session.menu().click(new GuiInteraction(player,element.action(),event.getClick(),session.page().page(),this)).whenComplete((ignored,failure)->{if(failure!=null)scheduler.executeEntity(player,()->player.sendMessage(Component.text("Menu action failed: "+root(failure).getMessage())),()->{});});}catch(Throwable failure){player.sendMessage(Component.text("Menu action failed: "+root(failure).getMessage()));}}
 @EventHandler(priority=EventPriority.HIGHEST)public void onDrag(InventoryDragEvent event){if(GuiSlotPolicy.cancelDrag(event.getView().getTopInventory().getHolder(false)instanceof Holder))event.setCancelled(true);}
 @EventHandler public void onClose(InventoryCloseEvent event){if(!(event.getInventory().getHolder(false)instanceof Holder holder))return;sessions.computeIfPresent(event.getPlayer().getUniqueId(),(id,session)->session.id().equals(holder.sessionId)?null:session);}
 @EventHandler public void onQuit(PlayerQuitEvent event){sessions.remove(event.getPlayer().getUniqueId());}
 @Override public void close(){HandlerList.unregisterAll(this);sessions.clear();menus.clear();}
 private static Throwable root(Throwable failure){Throwable value=failure;while(value.getCause()!=null)value=value.getCause();return value;}
 private record Session(UUID id,GuiMenu menu,GuiPage page){}
 private static final class Holder implements InventoryHolder{private final UUID sessionId;private final String menuId;private Inventory inventory;private Holder(UUID sessionId,String menuId){this.sessionId=sessionId;this.menuId=menuId;}@Override public Inventory getInventory(){return inventory;}}
}
