package com.magicstudios.magiccore.config;

import java.util.List;

@FunctionalInterface
public interface ConfigValidator<T> {
    List<ValidationIssue> validate(T candidate);
}
