package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.bootstrap.PhaseTwoPlayerListener;
import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.gemshop.GemProduct;
import com.magicstudios.magiccore.modules.gemshop.GemShopQuote;
import com.magicstudios.magiccore.modules.gemshop.GemShopService;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class GemShopGuiMenu implements GuiMenu {
    private final MenusFile config;private final GemShopService service;private final PhaseTwoPlayerListener deliveries;private final GuiItemFactory items;
    public GemShopGuiMenu(MenusFile config,GemShopService service,PhaseTwoPlayerListener deliveries,GuiItemFactory items){this.config=config;this.service=service;this.deliveries=deliveries;this.items=items;}
    @Override public String id(){return "gemshop";}
    @Override public CompletionStage<GuiPage>render(Player player,int requestedPage){List<GemProduct>products=service.products();int perPage=GuiLayouts.CONTENT_28.length,pages=Math.max(1,(products.size()+perPage-1)/perPage),page=Math.min(Math.max(0,requestedPage),pages-1);Map<Integer,GuiElement>elements=new LinkedHashMap<>();for(int offset=0;offset<perPage;offset++){int index=page*perPage+offset;if(index>=products.size())break;GemProduct product=products.get(index);elements.put(GuiLayouts.CONTENT_28[offset],new GuiElement(items.item(product.material(),"<green><b>"+GuiText.safe(product.displayName())+"</b>",List.of("<white>Receive "+product.amount()+" configured item(s)","<gray>Category: "+GuiText.safe(product.category()),"<gold>Price: "+product.priceMinor()+" gems",requirements(product),"","<green>► Click to review purchase")),"QUOTE:"+product.id()));}MenusFile.Layout layout=GuiLayouts.require(config,id());return CompletableFuture.completedFuture(new GuiPage(layout.title(),layout.rows(),page,pages,GuiLayouts.withBack(elements,layout,items,"main")));}
    private static String requirements(GemProduct value){if(value.minimumKills()==0&&value.minimumPlaytimeSeconds()==0&&(value.requiredCapability()==null||value.requiredCapability().isBlank()))return "<dark_gray>No additional requirements";return "<dark_gray>Requirements apply";}
    @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().startsWith("MENU:")){interaction.controller().open(interaction.player(),interaction.action().substring(5));return CompletableFuture.completedFuture(null);}if(!interaction.action().startsWith("QUOTE:"))return CompletableFuture.completedFuture(null);UUID playerId=interaction.player().getUniqueId();return service.quote(playerId,interaction.action().substring(6),"gui-gem-quote:"+playerId+":"+UUID.randomUUID()).thenAccept(quote->interaction.controller().open(interaction.player(),new Confirmation(config,service,deliveries,items,quote),0));}

    private static final class Confirmation implements GuiMenu {
        private final MenusFile config;private final GemShopService service;private final PhaseTwoPlayerListener deliveries;private final GuiItemFactory items;private final GemShopQuote quote;
        private Confirmation(MenusFile config,GemShopService service,PhaseTwoPlayerListener deliveries,GuiItemFactory items,GemShopQuote quote){this.config=config;this.service=service;this.deliveries=deliveries;this.items=items;this.quote=quote;}
        @Override public String id(){return "gemshop-confirm-"+quote.id();}
        @Override public CompletionStage<GuiPage>render(Player player,int ignored){MenusFile.Layout base=GuiLayouts.require(config,"gemshop");Map<Integer,GuiElement>elements=new LinkedHashMap<>();GemProduct product=quote.product();elements.put(22,new GuiElement(items.item(product.material(),"<green><b>"+GuiText.safe(product.displayName())+"</b>",List.of("<white>Quantity: "+product.amount(),"<gold>Total: "+product.priceMinor()+" gems","<dark_gray>Quote expires: "+quote.expiresAt())),""));elements.put(30,new GuiElement(items.item(config.theme().positiveMaterial(),"<green><b>Confirm Purchase</b>",List.of("<white>Debit "+product.priceMinor()+" gems","<gray>Items use the recovery-safe mailbox","","<green>► Click once to confirm")),"CONFIRM"));elements.put(32,new GuiElement(items.item(config.theme().negativeMaterial(),"<red><b>Cancel</b>",List.of("<white>No gems will be charged","","<red>► Return to the GemShop")),"MENU:gemshop"));return CompletableFuture.completedFuture(new GuiPage("<dark_gray>Shop | Confirm Purchase",base.rows(),0,1,elements));}
        @Override public CompletionStage<Void>click(GuiInteraction interaction){if(interaction.action().equals("MENU:gemshop")){interaction.controller().open(interaction.player(),"gemshop");return CompletableFuture.completedFuture(null);}if(!interaction.action().equals("CONFIRM"))return CompletableFuture.completedFuture(null);UUID playerId=interaction.player().getUniqueId();interaction.controller().close(interaction.player());return service.confirm(playerId,quote.id(),"gui-gem-confirm:"+quote.id()).thenAccept(receipt->{deliveries.deliverPending(interaction.player());interaction.player().sendMessage(net.kyori.adventure.text.Component.text("Purchase complete. Receipt "+receipt.id()+"; gem balance "+receipt.balanceAfterMinor()+"."));});}
    }
}
