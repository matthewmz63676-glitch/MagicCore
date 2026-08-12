package com.magicstudios.magiccore.modules.presentation;

import java.util.List;

public record NavigationView(String id, List<NavigationItemView> entries) {
    public NavigationView {
        entries = List.copyOf(entries);
    }
}
