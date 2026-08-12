package com.magicstudios.magiccore.integrations.items;

import org.bukkit.inventory.ItemStack;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CustomItemService {
    String provider();
    boolean available();
    CompletionStage<Optional<ItemStack>> create(String itemId,int amount);
}
