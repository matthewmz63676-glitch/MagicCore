package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.config.model.PresentationFile;
import com.magicstudios.magiccore.modules.afk.*;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.presentation.*;
import com.magicstudios.magiccore.modules.profiles.*;
import com.magicstudios.magiccore.modules.settings.*;
import com.magicstudios.magiccore.modules.statistics.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000222");
    private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

    @Test
    void filtersNavigationByCapabilityAndCalculatesApplicationProgress() {
        PresentationFile config = new PresentationFile(1,
                List.of(entry("rules", "")),
                List.of(entry("spawn", ""), entry("staff", "STAFF_NAVIGATION")),
                new PresentationFile.Applications(
                        new PresentationFile.ApplicationDefinition("Media", "https://example.com/media", List.of(
                                requirement("playtime", "PLAYTIME_SECONDS", 100, ""),
                                requirement("profile", "SETTING_ENABLED", 1, "PROFILE_PUBLIC"),
                                requirement("shards", "SHARDS", 20, ""))),
                        new PresentationFile.ApplicationDefinition("Staff", "https://example.com/staff", List.of(
                                requirement("access", "CAPABILITY", 1, "APPLICATION_STAFF")))));
        ConfiguredPresentationService service = new ConfiguredPresentationService(config, profiles(), stats(), settings(),
                capabilities(), shards(), Optional.empty(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.info(PLAYER).toCompletableFuture().join().entries()).extracting(NavigationItemView::id)
                .containsExactly("rules");
        assertThat(service.serverNavigation(PLAYER).toCompletableFuture().join().entries()).extracting(NavigationItemView::id)
                .containsExactly("spawn");
        ApplicationView media = service.application(PLAYER, ApplicationKind.MEDIA).toCompletableFuture().join();
        assertThat(media.eligible()).isTrue();
        assertThat(media.satisfiedRequirements()).isEqualTo(3);
        assertThat(media.requirements()).allMatch(RequirementProgress::satisfied);
        assertThat(service.application(PLAYER, ApplicationKind.STAFF).toCompletableFuture().join().eligible()).isTrue();
    }

    private static PresentationFile.NavigationEntry entry(String id, String capability) {
        return new PresentationFile.NavigationEntry(id, id, id + " description", "STONE", 1, "COMMAND:/" + id, capability);
    }

    private static PresentationFile.Requirement requirement(String id, String type, long target, String capability) {
        return new PresentationFile.Requirement(id, type, id, target, capability);
    }

    private static PlayerProfileService profiles() {
        PlayerProfile profile = new PlayerProfile(PLAYER, List.of("Player"), "en_US", NOW.minusSeconds(900_000), NOW, Map.of());
        return new PlayerProfileService() {
            public CompletionStage<PlayerProfile> recordSeen(UUID id,String name,String locale,Instant seen){return CompletableFuture.completedFuture(profile);}
            public CompletionStage<Optional<PlayerProfile>> find(UUID id){return CompletableFuture.completedFuture(Optional.of(profile));}
            public CompletionStage<PlayerProfile> setLocale(UUID id,String locale,String key){return CompletableFuture.completedFuture(profile);}
            public CompletionStage<PlayerProfile> setSetting(UUID id,String key,String value,String operation){return CompletableFuture.completedFuture(profile);}
        };
    }

    private static PlayerStatsService stats() {
        PlayerStats value = new PlayerStats(PLAYER, 5, 2, 120, NOW);
        return new PlayerStatsService() {
            public CompletionStage<PlayerStats> stats(UUID id){return CompletableFuture.completedFuture(value);}
            public CompletionStage<PlayerStats> recordKill(VerifiedPlayerKill kill,String key){return CompletableFuture.completedFuture(value);}
            public CompletionStage<PlayerStats> recordDeath(UUID id,UUID event,String key){return CompletableFuture.completedFuture(value);}
            public CompletionStage<PlayerStats> addPlaytime(UUID id,long seconds,String key){return CompletableFuture.completedFuture(value);}
            public CompletionStage<List<StatsLeaderboardEntry>> leaderboard(StatsMetric metric,int limit){return CompletableFuture.completedFuture(List.of());}
        };
    }

    private static PlayerSettingsService settings() {
        PlayerSettings value = new PlayerSettings(PLAYER, Map.of(PlayerSetting.PROFILE_PUBLIC, true), NOW);
        return new PlayerSettingsService() {
            public CompletionStage<PlayerSettings> get(UUID id){return CompletableFuture.completedFuture(value);}
            public CompletionStage<PlayerSettings> set(UUID id,PlayerSetting setting,boolean enabled,String key){return CompletableFuture.completedFuture(value);}
        };
    }

    private static CapabilityService capabilities() {
        return new CapabilityService() {
            public CompletionStage<Boolean> has(UUID id,String capability){return CompletableFuture.completedFuture(capability.equals("APPLICATION_STAFF"));}
            public CompletionStage<Integer> limit(UUID id,String limit){return CompletableFuture.completedFuture(0);}
            public CompletionStage<Boolean> canTarget(UUID actor,UUID target){return CompletableFuture.completedFuture(true);}
        };
    }

    private static ShardService shards() {
        ShardBalance value = new ShardBalance(PLAYER, 25, 25, LocalDate.of(2026, 8, 11), NOW);
        return new ShardService() {
            public CompletionStage<ShardBalance> balance(UUID id){return CompletableFuture.completedFuture(value);}
            public CompletionStage<ShardAwardResult> award(UUID id,AfkEligibilitySnapshot eligibility,String interval){throw new UnsupportedOperationException();}
            public CompletionStage<ShardBalance> adjust(UUID id,long delta,String reason,String key){throw new UnsupportedOperationException();}
            public CompletionStage<List<ShardTransaction>> history(UUID id,int limit){return CompletableFuture.completedFuture(List.of());}
        };
    }
}
