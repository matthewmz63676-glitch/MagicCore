package com.magicstudios.magiccore.modules.store;

import java.util.List;

public record ProductDefinition(String id, String displayName, long minimumPaidMinor, List<ProductAction> actions) {
    public ProductDefinition { actions = List.copyOf(actions); }
}
