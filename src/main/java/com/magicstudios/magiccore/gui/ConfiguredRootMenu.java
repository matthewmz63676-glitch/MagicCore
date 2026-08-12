package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.config.model.MenusFile;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ConfiguredRootMenu implements GuiMenu {
    private final MenusFile config;
    private final CapabilityService capabilities;
    private final GuiItemFactory items;
    private final java.util.function.Predicate<String> available;

    public ConfiguredRootMenu(MenusFile config, CapabilityService capabilities, GuiItemFactory items) {
        this(config,capabilities,items,ignored->true);
    }
    public ConfiguredRootMenu(MenusFile config,CapabilityService capabilities,GuiItemFactory items,java.util.function.Predicate<String>available){
        this.config=config;this.capabilities=capabilities;this.items=items;this.available=available;
    }

    @Override public String id() { return "main"; }

    @Override public CompletionStage<GuiPage> render(Player player, int ignored) {
        List<CompletableFuture<Boolean>> checks = config.rootEntries().stream().map(entry ->
                entry.requiredCapability() == null || entry.requiredCapability().isBlank()
                        ? CompletableFuture.completedFuture(true)
                        : capabilities.has(player.getUniqueId(), entry.requiredCapability()).toCompletableFuture()).toList();
        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(nothing -> {
            Map<Integer, GuiElement> elements = new LinkedHashMap<>();
            for (int index = 0; index < config.rootEntries().size(); index++) {
                MenusFile.RootEntry entry = config.rootEntries().get(index);
                if (checks.get(index).join()&&available.test(entry.menuId())) elements.put(entry.slot(), new GuiElement(
                        items.item(entry.material(), entry.name(), entry.lore()), "MENU:" + entry.menuId()));
            }
            MenusFile.Layout layout = GuiLayouts.require(config, id());
            return new GuiPage(layout.title(), layout.rows(), 0, 1, elements);
        });
    }

    @Override public CompletionStage<Void> click(GuiInteraction interaction) {
        if (interaction.action().startsWith("MENU:")) interaction.controller().open(interaction.player(), interaction.action().substring(5));
        return CompletableFuture.completedFuture(null);
    }
}
