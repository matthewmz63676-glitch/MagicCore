package com.magicstudios.magiccore.config;

import java.util.Objects;

public record ValidationIssue(String path, ValidationSeverity severity, String message, String smallestFix) {
    public ValidationIssue {
        path = Objects.requireNonNull(path, "path");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        smallestFix = Objects.requireNonNull(smallestFix, "smallestFix");
    }

    public static ValidationIssue error(String path, String message, String smallestFix) {
        return new ValidationIssue(path, ValidationSeverity.ERROR, message, smallestFix);
    }

    public static ValidationIssue warning(String path, String message, String smallestFix) {
        return new ValidationIssue(path, ValidationSeverity.WARNING, message, smallestFix);
    }
}
