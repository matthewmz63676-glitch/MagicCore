package com.magicstudios.magiccore.api;

import java.util.Objects;
import java.util.Optional;

public record Result<T>(boolean successful, String code, String message, T value) {
    public Result {
        code = requireCode(code);
        message = Objects.requireNonNull(message, "message");
        if (successful && value == null) {
            throw new IllegalArgumentException("A successful result requires a value");
        }
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(true, "OK", "", Objects.requireNonNull(value, "value"));
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    public Optional<T> optionalValue() {
        return Optional.ofNullable(value);
    }

    private static String requireCode(String value) {
        Objects.requireNonNull(value, "code");
        if (!value.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("Result code must be a stable upper-snake-case identifier: " + value);
        }
        return value;
    }
}
