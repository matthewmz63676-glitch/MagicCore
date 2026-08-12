package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class SellingGuiMenu implements GuiMenu {
    private final MenusFile config;private final GuiItemFactory items;
    public SellingGuiMenu(MenusFile config,GuiItemFactory items){this.config=config;this.items=items;}
    @Override public String id(){return "selling";}
    @Override public CompletionStage<GuiPage>render(Player player,int ignored){MenusFile.Layout layout=GuiLayouts.require(config,id());Map<Integer,GuiElement>elements=new LinkedHashMap<>();elements.put(20,new GuiElement(items.item("DIAMOND","<gold><b>Sell Held Item</b>",List.of("<white>Values the item in your main hand","<gray>Uses configured worth rules","","<gold>► Click to quote and sell")),"COMMAND:magic sell hand"));elements.put(22,new GuiElement(items.item("CHEST","<gold><b>Sell Inventory</b>",List.of("<white>Values eligible storage contents","<gray>Protected and excluded items remain","","<gold>► Click to quote and sell")),"COMMAND:magic sell all"));elements.put(24,new GuiElement(items.item("BOOK","<aqua><b>Sale History</b>",List.of("<white>Review recent persisted receipts","","<aqua>► Click to list history")),"COMMAND:magic sell history"));return CompletableFuture.completedFuture(new GuiPage(layout.title(),layout.rows(),0,1,GuiLayouts.withBack(elements,layout,items,"main")));}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:")){interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}if(interaction.action().startsWith("COMMAND:")){interaction.controller().close(interaction.player());interaction.player().performCommand(interaction.action().substring(8));}return CompletableFuture.completedFuture(null);}
}
