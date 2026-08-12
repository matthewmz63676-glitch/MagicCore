package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.events.KothRun;
import com.magicstudios.magiccore.modules.events.KothService;
import com.magicstudios.magiccore.modules.events.VotePartyService;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class EventStatusGuiMenu implements GuiMenu {
    private final MenusFile menus;private final KothService koth;private final VotePartyService votes;private final GuiItemFactory items;
    public EventStatusGuiMenu(MenusFile menus,KothService koth,VotePartyService votes,GuiItemFactory items){this.menus=menus;this.koth=koth;this.votes=votes;this.items=items;}
    @Override public String id(){return "events";}
    @Override public CompletionStage<GuiPage>render(Player player,int ignored){var kothFutures=koth.definitions().stream().map(value->koth.active(value.id()).toCompletableFuture()).toList();var state=votes.state().toCompletableFuture();var party=votes.activeParty().toCompletableFuture();java.util.List<CompletableFuture<?>>all=new java.util.ArrayList<>(kothFutures);all.add(state);all.add(party);return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).thenApply(nothing->{Map<Integer,GuiElement>elements=new LinkedHashMap<>();List<String>hillLore=new java.util.ArrayList<>(List.of("<white>Scheduled team-aware capture events"));if(kothFutures.isEmpty())hillLore.add("<dark_gray>No hills are currently enabled");else for(int index=0;index<kothFutures.size();index++){var definition=koth.definitions().get(index);hillLore.add(kothFutures.get(index).join().map(run->"<gold>"+GuiText.safe(definition.displayName())+": "+run.holdingName()+" "+run.capturedMillis()/1000+"s").orElse("<dark_gray>"+GuiText.safe(definition.displayName())+": inactive"));}elements.put(20,new GuiElement(items.item("BEACON","<gold><b>King of the Hill</b>",hillLore),""));elements.put(22,new GuiElement(items.item("PAPER","<aqua><b>Vote Party</b>",List.of("<white>Verified votes: "+state.join().count(),"<dark_gray>Threshold progress is persistent","<dark_gray>Offline policy is configurable")),""));elements.put(24,new GuiElement(items.item(party.join().isPresent()?"GOLDEN_APPLE":"LEAD",party.join().isPresent()?"<green><b>Pinata Active</b>":"<gray><b>Pinata Inactive</b>",party.join().map(value->List.of("<white>Remaining hits: "+value.remaining(),"<dark_gray>Per-player limits apply")).orElse(List.of("<white>The next verified threshold spawns it","<dark_gray>Rewards require policy eligibility"))),""));MenusFile.Layout layout=GuiLayouts.require(menus,id());return new GuiPage(layout.title(),layout.rows(),0,1,GuiLayouts.withBack(elements,layout,items,"main"));});}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:"))interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}
}
