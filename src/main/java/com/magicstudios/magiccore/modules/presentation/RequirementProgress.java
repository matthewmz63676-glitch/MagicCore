package com.magicstudios.magiccore.modules.presentation;

public record RequirementProgress(String id, String label, String type,
                                  long current, long target, boolean maximum,
                                  boolean satisfied, String detail) {
    public double fraction() {
        if (maximum) return satisfied ? 1.0D : Math.max(0.0D, Math.min(1.0D, (double) target / Math.max(1L, current)));
        if (target <= 0L) return 1.0D;
        return Math.max(0.0D, Math.min(1.0D, (double) current / target));
    }
}
