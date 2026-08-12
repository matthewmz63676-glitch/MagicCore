package com.magicstudios.magiccore.config.model;

import java.util.List;

public record DisplayFile(int configVersion, long refreshSeconds, Scoreboard scoreboard, Tab tab,
                          BelowName belowName, Chat chat, long leaderboardCacheSeconds) {
    public record Scoreboard(boolean enabled, String title, List<String> lines) {
        public Scoreboard { lines = List.copyOf(lines); }
    }
    public record Tab(boolean enabled, String header, String footer, String nameFormat) { }
    public record BelowName(boolean enabled, String label) { }
    public record Chat(boolean enabled, String format, boolean mentionsEnabled) { }
}
