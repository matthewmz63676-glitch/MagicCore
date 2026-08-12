package com.magicstudios.magiccore.capabilities;

import java.util.Locale;

public record Capability(String id) {
    public Capability {
        id = id.toUpperCase(Locale.ROOT);
        if (!id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Invalid capability ID: " + id);
    }
}
