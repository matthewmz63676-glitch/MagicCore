package com.magicstudios.magiccore.protection;

public record ProtectionDecision(boolean allowed, String provider, String reason) {
    public static ProtectionDecision allow(String provider) { return new ProtectionDecision(true, provider, "ALLOWED"); }
    public static ProtectionDecision deny(String provider, String reason) { return new ProtectionDecision(false, provider, reason); }
}
