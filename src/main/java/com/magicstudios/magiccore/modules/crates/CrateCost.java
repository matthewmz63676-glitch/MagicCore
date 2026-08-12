package com.magicstudios.magiccore.modules.crates;

public record CrateCost(Type type, String keyId, long amount) {
    public enum Type { KEY, CURRENCY }
    public CrateCost { if (amount < 1) throw new IllegalArgumentException("crate cost must be positive"); }
}
