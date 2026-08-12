package com.magicstudios.magiccore.config;

import java.util.List;

public final class ConfigValidationException extends RuntimeException {
    private final List<ValidationIssue> issues;

    public ConfigValidationException(List<ValidationIssue> issues) {
        super(issues.stream().map(issue -> issue.path() + ": " + issue.message()
                + " Fix: " + issue.smallestFix()).reduce((a, b) -> a + "; " + b).orElse("Invalid configuration"));
        this.issues = List.copyOf(issues);
    }

    public List<ValidationIssue> issues() {
        return issues;
    }
}
