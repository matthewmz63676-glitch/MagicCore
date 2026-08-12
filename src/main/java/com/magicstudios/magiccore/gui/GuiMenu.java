package com.magicstudios.magiccore.gui;

import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface GuiMenu {
 String id();
 CompletionStage<GuiPage>render(Player player,int page);
 default CompletionStage<Void>click(GuiInteraction interaction){return CompletableFuture.completedFuture(null);}
}
