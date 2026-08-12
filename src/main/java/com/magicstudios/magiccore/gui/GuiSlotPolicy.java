package com.magicstudios.magiccore.gui;

/** Pure inventory-boundary policy used by the Bukkit listener and regression tests. */
public final class GuiSlotPolicy {
    private GuiSlotPolicy() { }

    public static boolean cancelClick(boolean magicInventory) {
        return magicInventory;
    }

    public static boolean actionableTopSlot(boolean magicInventory, int rawSlot, int topSize) {
        return magicInventory && rawSlot >= 0 && rawSlot < topSize;
    }

    public static boolean cancelDrag(boolean magicInventory) {
        return magicInventory;
    }

    public static int previousOrBackSlot(int inventorySize) {
        return inventorySize - 9;
    }

    public static int closeSlot(int inventorySize) {
        return inventorySize - 5;
    }

    public static int nextSlot(int inventorySize) {
        return inventorySize - 1;
    }
}
