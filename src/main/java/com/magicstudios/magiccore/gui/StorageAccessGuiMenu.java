package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.config.model.SecureStorageFile;
import com.magicstudios.magiccore.modules.securestorage.VirtualContainer;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class StorageAccessGuiMenu implements GuiMenu {
    private final MenusFile menus;private final SecureStorageFile policy;private final SecureStorageController storage;private final GuiItemFactory items;
    public StorageAccessGuiMenu(MenusFile menus,SecureStorageFile policy,SecureStorageController storage,GuiItemFactory items){this.menus=menus;this.policy=policy;this.storage=storage;this.items=items;}
    @Override public String id(){return "storage";}
    @Override public CompletionStage<GuiPage>render(Player player,int requestedPage){int perPage=GuiLayouts.CONTENT_28.length,total=policy.maximumVaults()+1,pages=Math.max(1,(total+perPage-1)/perPage),page=Math.min(Math.max(0,requestedPage),pages-1);Map<Integer,GuiElement>elements=new LinkedHashMap<>();for(int offset=0;offset<perPage;offset++){int index=page*perPage+offset;if(index>=total)break;if(index==0)elements.put(GuiLayouts.CONTENT_28[offset],new GuiElement(items.item("ENDER_CHEST","<light_purple><b>Secure Ender Chest</b>",List.of("<white>Recovery-safe 27-slot storage","<dark_gray>One exclusive session at a time","","<light_purple>► Click to open")),"OPEN:ENDER_CHEST:0"));else elements.put(GuiLayouts.CONTENT_28[offset],new GuiElement(items.item("CHEST","<aqua><b>Vault "+index+"</b>",List.of("<white>Size follows your VAULT_ROWS entitlement","<dark_gray>Atomic save with lease protection","","<aqua>► Click to open")),"OPEN:VAULT:"+index));}MenusFile.Layout layout=GuiLayouts.require(menus,id());return CompletableFuture.completedFuture(new GuiPage(layout.title(),layout.rows(),page,pages,GuiLayouts.withBack(elements,layout,items,"main")));}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:")){interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}if(interaction.action().startsWith("OPEN:")){String[]parts=interaction.action().split(":");interaction.controller().close(interaction.player());storage.open(interaction.player(),VirtualContainer.Type.valueOf(parts[1]),Integer.parseInt(parts[2]));}return CompletableFuture.completedFuture(null);}
}
