package com.magicstudios.magiccore.admin;

import java.util.Objects;
import java.util.UUID;

public record AdminActor(UUID playerId, String displayName, boolean console) {
    public AdminActor {
        displayName = Objects.requireNonNull(displayName, "displayName");
        if (!console && playerId == null) throw new IllegalArgumentException("A player actor requires a UUID");
    }

    public static AdminActor consoleActor() {
        return new AdminActor(null, "CONSOLE", true);
    }
}
