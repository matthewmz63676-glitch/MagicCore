package com.magicstudios.magiccore.modules.essentials;

public record TeleportResult(boolean completed, String code) {
    public static TeleportResult success() { return new TeleportResult(true, "TELEPORTED"); }
    public static TeleportResult rejected(String code) { return new TeleportResult(false, code); }
}
