package com.magicstudios.magiccore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public record GuiInteraction(Player player,String action,ClickType click,int page,MagicGuiController controller) { }
