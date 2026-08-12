package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpQuery;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpService;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpView;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PlayerWarpsGuiMenu implements GuiMenu {
    private final MenusFile config;private final PlayerWarpService service;private final GuiItemFactory items;
    public PlayerWarpsGuiMenu(MenusFile config,PlayerWarpService service,GuiItemFactory items){this.config=config;this.service=service;this.items=items;}
    @Override public String id(){return "playerwarps";}
    @Override public CompletionStage<GuiPage>render(Player player,int requestedPage){int page=Math.max(0,requestedPage),limit=GuiLayouts.CONTENT_28.length;PlayerWarpQuery query=new PlayerWarpQuery("","",null,player.getUniqueId(),PlayerWarpQuery.Sort.SPONSORED,page*limit,limit);return service.search(query).thenCompose(values->{CompletionStage<List<PlayerWarpView>>resolved=values.size()==limit?service.search(new PlayerWarpQuery("","",null,player.getUniqueId(),PlayerWarpQuery.Sort.SPONSORED,(page+1)*limit,1)):CompletableFuture.completedFuture(List.of());return resolved.thenApply(next->{int pages=next.isEmpty()?page+1:page+2;Map<Integer,GuiElement>elements=new LinkedHashMap<>();for(int offset=0;offset<values.size();offset++){PlayerWarpView view=values.get(offset);var warp=view.warp();String state=view.promoted()?"<gold>Sponsored destination":view.favorite()?"<yellow>Favorite destination":"<dark_gray>Community destination";elements.put(GuiLayouts.CONTENT_28[offset],new GuiElement(items.item(view.promoted()?"GOLDEN_CARROT":"ENDER_PEARL",(view.promoted()?"<gold>":"<aqua>")+"<b>"+GuiText.safe(warp.displayName())+"</b>",List.of("<white>Category: "+GuiText.safe(warp.category()),"<gray>Visits: "+warp.visits(),state,"","<aqua>► Left-click to visit",view.favorite()?"<red>► Right-click to unfavorite":"<yellow>► Right-click to favorite")),"WARP:"+warp.id()+":"+view.favorite()));}MenusFile.Layout layout=GuiLayouts.require(config,id());return new GuiPage(layout.title(),layout.rows(),page,pages,GuiLayouts.withBack(elements,layout,items,"main"));});});}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:")){interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}if(!interaction.action().startsWith("WARP:"))return CompletableFuture.completedFuture(null);String data=interaction.action().substring(5);int split=data.lastIndexOf(':');String id=data.substring(0,split);boolean favorite=Boolean.parseBoolean(data.substring(split+1));if(interaction.click().isRightClick()){UUID playerId=interaction.player().getUniqueId();return service.favorite(playerId,id,!favorite,"gui-pwarp-favorite:"+playerId+":"+id+":"+UUID.randomUUID()).thenAccept(changed->interaction.controller().refresh(interaction.player()));}interaction.controller().close(interaction.player());interaction.player().performCommand("magic pwarp "+id);return CompletableFuture.completedFuture(null);}
}
