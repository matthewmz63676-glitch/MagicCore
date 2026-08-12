package com.magicstudios.magiccore.modules.presentation;

import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.config.model.PresentationFile;
import com.magicstudios.magiccore.integrations.discord.DiscordIntegrationService;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.profiles.PlayerProfile;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettings;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.modules.statistics.PlayerStats;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ConfiguredPresentationService implements PresentationService {
    private final PresentationFile config;
    private final PlayerProfileService profiles;
    private final PlayerStatsService stats;
    private final PlayerSettingsService settings;
    private final CapabilityService capabilities;
    private final ShardService shards;
    private final Optional<DiscordIntegrationService> discord;
    private final Clock clock;

    public ConfiguredPresentationService(PresentationFile config, PlayerProfileService profiles,
                                         PlayerStatsService stats, PlayerSettingsService settings,
                                         CapabilityService capabilities, ShardService shards,
                                         Optional<DiscordIntegrationService> discord, Clock clock) {
        this.config = config;
        this.profiles = profiles;
        this.stats = stats;
        this.settings = settings;
        this.capabilities = capabilities;
        this.shards = shards;
        this.discord = discord;
        this.clock = clock;
    }

    @Override
    public CompletionStage<NavigationView> info(UUID playerId) {
        return navigation("info", playerId, config.info());
    }

    @Override
    public CompletionStage<NavigationView> serverNavigation(UUID playerId) {
        return navigation("server", playerId, config.serverNavigation());
    }

    private CompletionStage<NavigationView> navigation(String id, UUID playerId,
                                                        List<PresentationFile.NavigationEntry> definitions) {
        List<CompletableFuture<Optional<NavigationItemView>>> checks = definitions.stream().map(entry -> {
            String required = normalized(entry.requiredCapability());
            CompletionStage<Boolean> allowed = required.isEmpty()
                    ? CompletableFuture.completedFuture(true) : capabilities.has(playerId, required);
            return allowed.thenApply(show -> show ? Optional.of(new NavigationItemView(entry.id(), entry.title(),
                    entry.description(), entry.material(), entry.slot(), entry.action())) : Optional.<NavigationItemView>empty())
                    .toCompletableFuture();
        }).toList();
        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(ignored ->
                new NavigationView(id, checks.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList()));
    }

    @Override
    public CompletionStage<ApplicationView> application(UUID playerId, ApplicationKind kind) {
        PresentationFile.ApplicationDefinition definition = kind == ApplicationKind.MEDIA
                ? config.applications().media() : config.applications().staff();
        CompletableFuture<PlayerStats> statsFuture = stats.stats(playerId).toCompletableFuture();
        CompletableFuture<Optional<PlayerProfile>> profileFuture = profiles.find(playerId).toCompletableFuture();
        CompletableFuture<PlayerSettings> settingsFuture = settings.get(playerId).toCompletableFuture();
        CompletableFuture<Long> shardFuture = shards.balance(playerId).thenApply(value -> value.amount()).toCompletableFuture();
        CompletableFuture<Optional<String>> discordFuture = discord
                .map(service -> service.linkedDiscordId(playerId).toCompletableFuture())
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));

        return CompletableFuture.allOf(statsFuture, profileFuture, settingsFuture, shardFuture, discordFuture)
                .thenCompose(ignored -> evaluate(playerId, kind, definition, statsFuture.join(), profileFuture.join(),
                        settingsFuture.join(), shardFuture.join(), discordFuture.join()));
    }

    private CompletionStage<ApplicationView> evaluate(UUID playerId, ApplicationKind kind,
                                                       PresentationFile.ApplicationDefinition definition,
                                                       PlayerStats playerStats, Optional<PlayerProfile> profile,
                                                       PlayerSettings playerSettings, long shardBalance,
                                                       Optional<String> linkedDiscord) {
        List<CompletableFuture<RequirementProgress>> checks = new ArrayList<>();
        for (PresentationFile.Requirement requirement : definition.requirements()) {
            String type = requirement.type().toUpperCase(Locale.ROOT);
            if (type.equals("CAPABILITY")) {
                checks.add(capabilities.has(playerId, requirement.capability()).thenApply(value -> progress(requirement,
                        value ? 1L : 0L, false, value, value ? "granted" : "missing")).toCompletableFuture());
                continue;
            }
            long current;
            boolean maximum = false;
            String detail;
            switch (type) {
                case "PLAYTIME_SECONDS" -> { current = playerStats.playtimeSeconds(); detail = current + " seconds"; }
                case "KILLS" -> { current = playerStats.kills(); detail = current + " kills"; }
                case "DEATHS" -> { current = playerStats.deaths(); detail = current + " deaths"; }
                case "DEATHS_MAXIMUM" -> { current = playerStats.deaths(); maximum = true; detail = current + " / maximum " + requirement.target(); }
                case "ACCOUNT_AGE_SECONDS" -> {
                    current = profile.map(value -> Math.max(0L, Duration.between(value.firstSeen(), clock.instant()).toSeconds())).orElse(0L);
                    detail = current + " seconds";
                }
                case "SHARDS" -> { current = shardBalance; detail = current + " shards"; }
                case "DISCORD_LINKED" -> { current = linkedDiscord.isPresent() ? 1L : 0L; detail = linkedDiscord.isPresent() ? "linked" : "not linked"; }
                case "SETTING_ENABLED" -> {
                    PlayerSetting setting = PlayerSetting.valueOf(requirement.capability().toUpperCase(Locale.ROOT));
                    current = playerSettings.enabled(setting) ? 1L : 0L;
                    detail = current == 1L ? "enabled" : "disabled";
                }
                default -> throw new IllegalStateException("Unsupported application requirement type: " + type);
            }
            boolean satisfied = maximum ? current <= requirement.target() : current >= requirement.target();
            checks.add(CompletableFuture.completedFuture(progress(requirement, current, maximum, satisfied, detail)));
        }
        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<RequirementProgress> results = checks.stream().map(CompletableFuture::join).toList();
            int satisfied = (int) results.stream().filter(RequirementProgress::satisfied).count();
            return new ApplicationView(kind, definition.title(), definition.applyUrl(), satisfied == results.size(),
                    satisfied, results);
        });
    }

    private static RequirementProgress progress(PresentationFile.Requirement definition, long current,
                                                boolean maximum, boolean satisfied, String detail) {
        return new RequirementProgress(definition.id(), definition.label(), definition.type(), current,
                definition.target(), maximum, satisfied, detail);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
