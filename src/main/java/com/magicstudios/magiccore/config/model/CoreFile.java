package com.magicstudios.magiccore.config.model;

public record CoreFile(int configVersion, String serverName, String preset,
                       String language, Safety safety, Io io) {
    public record Safety(boolean requireConfirmationForDestructiveActions, boolean rejectMainThreadIo) { }
    public record Io(int threads, int queueCapacity) { }
}
