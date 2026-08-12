package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import org.bukkit.inventory.ItemStack;

public final class BukkitItemFingerprint {
    private BukkitItemFingerprint() { }
    public static ItemFingerprint fingerprint(ItemStack item) {
        ItemStack canonical = item.clone();
        canonical.setAmount(1);
        return ItemFingerprint.of(canonical.getType().getKey().asString(), canonical.serializeAsBytes());
    }
}
