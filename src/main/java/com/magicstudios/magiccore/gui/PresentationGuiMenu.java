package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.modules.presentation.NavigationItemView;
import com.magicstudios.magiccore.modules.presentation.NavigationView;
import com.magicstudios.magiccore.modules.presentation.PresentationService;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PresentationGuiMenu implements GuiMenu {
    private final String id;
    private final MenusFile config;
    private final PresentationService service;
    private final GuiItemFactory items;

    public PresentationGuiMenu(String id, MenusFile config, PresentationService service, GuiItemFactory items) {
        if (!id.equals("info") && !id.equals("server")) throw new IllegalArgumentException("Unsupported presentation menu");
        this.id = id; this.config = config; this.service = service; this.items = items;
    }

    @Override public String id() { return id; }

    @Override public CompletionStage<GuiPage> render(Player player, int ignored) {
        CompletionStage<NavigationView> view = id.equals("info") ? service.info(player.getUniqueId()) : service.serverNavigation(player.getUniqueId());
        return view.thenApply(value -> {
            Map<Integer, GuiElement> elements = new LinkedHashMap<>();
            for (NavigationItemView entry : value.entries()) elements.put(entry.slot(), new GuiElement(
                    items.item(entry.material(), "<aqua><b>" + GuiText.safe(entry.title()) + "</b>",
                            List.of("<white>" + GuiText.safe(entry.description()), "", "<aqua>► Click to continue")), entry.action()));
            MenusFile.Layout layout = GuiLayouts.require(config, id);
            return new GuiPage(layout.title(), layout.rows(), 0, 1, GuiLayouts.withBack(elements, layout, items, "main"));
        });
    }

    @Override public CompletionStage<Void> click(GuiInteraction interaction) {
        String action = interaction.action();
        if (action.startsWith("MENU:")) interaction.controller().open(interaction.player(), action.substring(5));
        else if (action.startsWith("COMMAND:")) {
            String command = action.substring(8);
            if (command.startsWith("/")) command = command.substring(1);
            interaction.player().performCommand(command);
            interaction.controller().close(interaction.player());
        }
        return CompletableFuture.completedFuture(null);
    }
}
