package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class SettingsGuiMenu implements GuiMenu {
    private final MenusFile config; private final PlayerSettingsService service; private final GuiItemFactory items;
    public SettingsGuiMenu(MenusFile config, PlayerSettingsService service, GuiItemFactory items) { this.config=config;this.service=service;this.items=items; }
    @Override public String id(){return "settings";}
    @Override public CompletionStage<GuiPage>render(Player player,int requestedPage){return service.get(player.getUniqueId()).thenApply(settings->{
        int perPage=GuiLayouts.CONTENT_28.length,total=PlayerSetting.values().length,pages=Math.max(1,(total+perPage-1)/perPage),page=Math.min(Math.max(0,requestedPage),pages-1);
        Map<Integer,GuiElement>elements=new LinkedHashMap<>();for(int offset=0;offset<perPage;offset++){int index=page*perPage+offset;if(index>=total)break;PlayerSetting setting=PlayerSetting.values()[index];boolean enabled=settings.enabled(setting);elements.put(GuiLayouts.CONTENT_28[offset],new GuiElement(items.item(enabled?config.theme().positiveMaterial():config.theme().negativeMaterial(),(enabled?"<green>":"<red>")+"<b>"+GuiText.label(setting.name())+"</b>",List.of("<white>Controls this personal preference","<dark_gray>Current: "+(enabled?"enabled":"disabled"),"",enabled?"<red>► Click to disable":"<green>► Click to enable")),"TOGGLE:"+setting.name()+":"+!enabled));}
        MenusFile.Layout layout=GuiLayouts.require(config,id());return new GuiPage(layout.title(),layout.rows(),page,pages,GuiLayouts.withBack(elements,layout,items,"main"));});}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:")){interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}if(!interaction.action().startsWith("TOGGLE:"))return CompletableFuture.completedFuture(null);String[]parts=interaction.action().split(":",3);PlayerSetting setting=PlayerSetting.valueOf(parts[1]);boolean enabled=Boolean.parseBoolean(parts[2]);UUID playerId=interaction.player().getUniqueId();return service.set(playerId,setting,enabled,"gui-setting:"+playerId+":"+setting+":"+UUID.randomUUID()).thenAccept(value->interaction.controller().refresh(interaction.player()));}
}
