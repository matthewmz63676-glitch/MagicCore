package com.magicstudios.magiccore.commands;

public final class CommandConflictException extends RuntimeException {
    public CommandConflictException(String message) {
        super(message);
    }
}
