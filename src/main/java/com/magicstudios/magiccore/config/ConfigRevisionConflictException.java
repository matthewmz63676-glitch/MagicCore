package com.magicstudios.magiccore.config;

public final class ConfigRevisionConflictException extends RuntimeException {
    public ConfigRevisionConflictException(long expected, long actual) {
        super("Configuration revision conflict: expected " + expected + " but active revision is " + actual
                + ". Reload the editor and reapply the change.");
    }
}
