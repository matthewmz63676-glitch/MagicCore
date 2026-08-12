package com.magicstudios.magiccore.commands;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CommandSpec(String owner, String primaryAlias, Set<String> aliases, String capability) {
    public CommandSpec {
        owner = requireToken(owner, "owner");
        primaryAlias = normalizeAlias(primaryAlias);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String alias : aliases) {
            normalized.add(normalizeAlias(alias));
        }
        normalized.remove(primaryAlias);
        aliases = Set.copyOf(normalized);
        capability = Objects.requireNonNull(capability, "capability");
    }

    public Set<String> allAliases() {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.add(primaryAlias);
        all.addAll(aliases);
        return Set.copyOf(all);
    }

    private static String normalizeAlias(String alias) {
        String value = requireToken(alias, "alias").toLowerCase(Locale.ROOT);
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private static String requireToken(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return value;
    }
}
