package com.magicstudios.magiccore.modules.settings;

public enum PlayerSetting {
    TELEPORT_REQUESTS(true),
    TRADE_REQUESTS(true),
    TEAM_INVITES(true),
    PRIVATE_MESSAGES(true),
    MENTIONS(true),
    PLAYER_VISIBILITY(true),
    SOUNDS(true),
    PARTICLES(true),
    SCOREBOARD(true),
    BOSSBAR(true),
    ANNOUNCEMENTS(true),
    VOTE_REMINDERS(true),
    WORTH_DISPLAY(true),
    PROFILE_PUBLIC(true),
    LUNAR_ENHANCEMENTS(true),
    DISCORD_SYNC(true),
    FAST_CRYSTAL(false),
    PHANTOMS(true),
    TEAM_CHAT(false);

    private final boolean defaultValue;

    PlayerSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
