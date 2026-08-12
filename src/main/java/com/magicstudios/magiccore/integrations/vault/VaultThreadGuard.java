package com.magicstudios.magiccore.integrations.vault;

import org.bukkit.Bukkit;

public final class VaultThreadGuard {
    private VaultThreadGuard() {
    }

    public static boolean isTickThread() {
        if (Bukkit.isPrimaryThread()) return true;
        String name = Thread.currentThread().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("region scheduler") || name.contains("tick thread") || name.contains("server thread");
    }
}
