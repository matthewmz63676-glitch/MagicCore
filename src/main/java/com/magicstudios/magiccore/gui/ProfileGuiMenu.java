package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.profiles.ProfileView;
import com.magicstudios.magiccore.modules.profiles.ProfileViewService;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ProfileGuiMenu implements GuiMenu {
    private final MenusFile config;private final ProfileViewService service;private final GuiItemFactory items;
    public ProfileGuiMenu(MenusFile config,ProfileViewService service,GuiItemFactory items){this.config=config;this.service=service;this.items=items;}
    @Override public String id(){return "profile";}
    @Override public CompletionStage<GuiPage>render(Player player,int ignored){return service.view(player.getUniqueId(),player.getUniqueId()).thenApply(view->{MenusFile.Layout layout=GuiLayouts.require(config,id());Map<Integer,GuiElement>elements=new LinkedHashMap<>();if(!view.visible())elements.put(22,new GuiElement(items.item("BARRIER","<red><b>Profile unavailable</b>",List.of("<gray>Privacy policy prevented this view")),""));else add(view,elements);return new GuiPage(layout.title(),layout.rows(),0,1,GuiLayouts.withBack(elements,layout,items,"main"));});}
    private void add(ProfileView view,Map<Integer,GuiElement>elements){long hours=Duration.ofSeconds(view.playtimeSeconds()).toHours();elements.put(13,new GuiElement(items.item("PLAYER_HEAD","<light_purple><b>"+GuiText.safe(view.currentName())+"</b>",List.of("<white>Your persistent MagicCore profile","<dark_gray>Rank: "+GuiText.safe(view.rankId()),"<dark_gray>First seen: "+view.firstSeen())),""));elements.put(20,new GuiElement(items.item("DIAMOND_SWORD","<red><b>Combat Record</b>",List.of("<white>Kills: "+view.kills(),"<white>Deaths: "+view.deaths())),""));elements.put(22,new GuiElement(items.item("CLOCK","<yellow><b>Playtime</b>",List.of("<white>"+hours+" completed hours","<dark_gray>Tracked server-side")),""));elements.put(24,new GuiElement(items.item("AMETHYST_SHARD","<light_purple><b>Shards</b>",List.of("<white>Balance: "+view.shards(),"<dark_gray>Earned through configured activities")),""));elements.put(31,new GuiElement(items.item("COMPARATOR","<aqua><b>Profile Settings</b>",List.of("<white>Manage privacy and notifications","","<aqua>► Click to open")),"MENU:settings"));}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:"))interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}
}
