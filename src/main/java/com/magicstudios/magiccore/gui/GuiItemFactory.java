package com.magicstudios.magiccore.gui;

import com.magicstudios.magiccore.text.MiniMessageRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public final class GuiItemFactory {
 private final MiniMessageRenderer renderer=new MiniMessageRenderer();
 public ItemStack item(String material,String name,List<String>lore){ItemStack item=new ItemStack(Material.valueOf(material));item.editMeta(meta->{meta.displayName(component(name));meta.lore(lore.stream().map(this::component).toList());meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_ADDITIONAL_TOOLTIP);});return item;}
 public ItemStack blank(String material){ItemStack item=new ItemStack(Material.valueOf(material));item.editMeta(meta->{meta.displayName(Component.empty());meta.lore(List.of());meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);});return item;}
 public Component component(String template){return renderer.render(GuiMarkup.complete(template));}
}
