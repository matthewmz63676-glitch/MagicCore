package com.magicstudios.magiccore.modules.securestorage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VirtualContainer(UUID ownerId, Type type, int index, int size, List<StoredItem> items,
                               Instant updatedAt) {
    public enum Type { VAULT, ENDER_CHEST }
    public VirtualContainer { items=List.copyOf(items); }
}
