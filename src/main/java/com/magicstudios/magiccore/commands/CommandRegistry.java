package com.magicstudios.magiccore.commands;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandRegistry {
    private final Map<String, Claim> aliases = new LinkedHashMap<>();

    public synchronized void register(CommandSpec spec) {
        for (String alias : spec.allAliases()) {
            Claim existing = aliases.get(alias);
            if (existing != null && !existing.owner().equals(spec.owner())) {
                throw conflict(alias, existing.owner(), spec.owner());
            }
        }
        spec.allAliases().forEach(alias -> aliases.put(alias, new Claim(spec.owner(), spec)));
    }

    public synchronized void observeExternal(String pluginName, String alias) {
        Objects.requireNonNull(pluginName, "pluginName");
        String normalized = normalize(alias);
        Claim existing = aliases.get(normalized);
        if (existing != null && !existing.owner().equals("external:" + pluginName)) {
            throw conflict(normalized, existing.owner(), "external:" + pluginName);
        }
        aliases.put(normalized, new Claim("external:" + pluginName, null));
    }

    public synchronized Optional<String> ownerOf(String alias) {
        Claim claim = aliases.get(normalize(alias));
        return claim == null ? Optional.empty() : Optional.of(claim.owner());
    }

    public synchronized Map<String, String> snapshot() {
        Map<String, String> result = new LinkedHashMap<>();
        aliases.forEach((alias, claim) -> result.put(alias, claim.owner()));
        return Map.copyOf(result);
    }

    public synchronized void unregisterOwner(String owner) {
        aliases.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
    }

    private static CommandConflictException conflict(String alias, String currentOwner, String candidateOwner) {
        return new CommandConflictException("Alias /" + alias + " is owned by " + currentOwner
                + "; " + candidateOwner + " must choose an explicit alternative such as /magic-" + alias);
    }

    private static String normalize(String alias) {
        String normalized = Objects.requireNonNull(alias, "alias").toLowerCase(Locale.ROOT);
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private record Claim(String owner, CommandSpec spec) {
    }
}
