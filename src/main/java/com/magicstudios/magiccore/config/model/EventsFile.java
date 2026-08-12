package com.magicstudios.magiccore.config.model;

import java.util.List;

public record EventsFile(int configVersion, Koth koth, VoteParty voteParty,
                         List<ScheduledAnnouncement> announcements, Maintenance maintenance) {
    public EventsFile { announcements = List.copyOf(announcements); }

    public record Koth(boolean enabled, int tickSeconds, List<KothDefinition> definitions) {
        public Koth { definitions = List.copyOf(definitions); }
    }
    public record KothDefinition(String id, boolean enabled, String displayName, String world,
                                 double minimumX, double minimumY, double minimumZ,
                                 double maximumX, double maximumY, double maximumZ,
                                 int captureSeconds, int firstDelaySeconds, int scheduleIntervalSeconds,
                                 List<String> bannedMaterials, EventReward reward) {
        public KothDefinition { bannedMaterials = List.copyOf(bannedMaterials); }
    }
    public record EventReward(String currency, long amountMinor) { }

    public record VoteParty(boolean enabled, int threshold, String offlinePolicy,
                            boolean requireVoterForRewards, Pinata pinata) { }
    public record Pinata(String world, double x, double y, double z, String entityType,
                         int maximumHits, int maximumHitsPerPlayer, EventReward hitReward,
                         EventReward finalReward, String bossbarTitle) { }

    public record ScheduledAnnouncement(String id, boolean enabled, int firstDelaySeconds,
                                        int intervalSeconds, String message, String sound,
                                        String title, String subtitle) { }
    public record Maintenance(int sponsorshipExpiryIntervalSeconds,
                              int secureStorageRecoveryIntervalSeconds) { }
}
