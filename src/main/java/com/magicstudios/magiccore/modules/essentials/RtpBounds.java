package com.magicstudios.magiccore.modules.essentials;

public record RtpBounds(double centerX, double centerZ, double minimumRadius, double maximumRadius,
                        int maximumAttempts) {
    public RtpBounds {
        if (minimumRadius < 0 || maximumRadius <= minimumRadius) throw new IllegalArgumentException("Invalid RTP radii");
        if (maximumAttempts < 1 || maximumAttempts > 1000) throw new IllegalArgumentException("maximumAttempts must be 1..1000");
    }
}
