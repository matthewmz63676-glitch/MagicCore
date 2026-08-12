package com.magicstudios.magiccore.modules.teams;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TeamNamePolicy {
    private final int minimumLength;
    private final int maximumLength;
    private final Pattern pattern;
    private final Set<String> blockedNames;

    public TeamNamePolicy(int minimumLength, int maximumLength, String pattern, Set<String> blockedNames) {
        if (minimumLength < 1 || maximumLength < minimumLength || maximumLength > 32) {
            throw new IllegalArgumentException("Team name lengths are invalid");
        }
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
        this.pattern = Pattern.compile("^(?:" + pattern + ")$");
        this.blockedNames = blockedNames.stream().map(name -> name.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String validate(String name) {
        if (name == null || name.length() < minimumLength || name.length() > maximumLength) {
            throw new IllegalArgumentException("Team name must contain " + minimumLength + ".." + maximumLength + " characters");
        }
        if (!pattern.matcher(name).matches()) throw new IllegalArgumentException("Team name does not match the configured character policy");
        if (blockedNames.contains(normalize(name))) throw new IllegalArgumentException("Team name is reserved");
        return name;
    }

    public String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public int maximumLength() {
        return maximumLength;
    }
}
