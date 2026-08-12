package com.magicstudios.magiccore.modules.kits;

import java.time.Duration;
import java.util.List;

public record KitDefinition(String id, String displayName, Duration cooldown, String capability,
                            List<KitItem> items) {
    public KitDefinition {
        if (id == null || !id.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Invalid kit ID");
        items = List.copyOf(items);
    }

    public record KitItem(String material, int amount, String itemDataBase64) {
        public KitItem {
            if (amount < 1 || amount > 64) throw new IllegalArgumentException("Kit item amount must be 1..64");
        }
    }
}
