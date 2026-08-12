package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.config.model.MenusFile;
import java.util.LinkedHashMap;
import java.util.Map;

final class GuiLayouts {
    static final int[] CONTENT_28 = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private GuiLayouts() { }

    static MenusFile.Layout require(MenusFile menus, String id) {
        MenusFile.Layout layout = menus.layouts().get(id);
        if (layout == null) throw new IllegalStateException("Missing menu layout " + id);
        return layout;
    }

    static Map<Integer, GuiElement> withBack(Map<Integer, GuiElement> source, MenusFile.Layout layout,
                                             GuiItemFactory items, String destination) {
        Map<Integer, GuiElement> result = new LinkedHashMap<>(source);
        result.put(layout.rows() * 9 - 9, new GuiElement(items.item("ARROW", "<aqua>◄ Back",
                java.util.List.of("<gray>Return to the previous menu")), "MENU:" + destination));
        return result;
    }
}
