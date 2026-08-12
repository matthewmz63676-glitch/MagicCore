package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.economy.BalanceChanged;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import com.magicstudios.magiccore.modules.rewards.RewardClaimed;
import com.magicstudios.magiccore.modules.rewards.RewardService;
import com.magicstudios.magiccore.modules.crates.CrateOpened;
import com.magicstudios.magiccore.modules.teams.TeamChanged;
import com.magicstudios.magiccore.modules.teams.TeamService;
import com.magicstudios.magiccore.ranks.RankChanged;
import com.magicstudios.magiccore.ranks.RankService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public final class PhaseOnePlaceholderView {
    private final PlayerProfileService profiles;
    private final EconomyService economy;
    private final RankService ranks;
    private final TeamService teams;
    private final RewardService rewards;
    private final Map<UUID, Snapshot> cache = new ConcurrentHashMap<>();

    public PhaseOnePlaceholderView(PlayerProfileService profiles, EconomyService economy, RankService ranks,
                                   TeamService teams, RewardService rewards) {
        this.profiles = profiles;
        this.economy = economy;
        this.ranks = ranks;
        this.teams = teams;
        this.rewards = rewards;
    }

    public void register(String owner, PlaceholderRegistry registry, DomainEventBus events) {
        if (profiles != null) {
            registry.register(owner, "profiles_name", context -> value(context, Snapshot::name));
            registry.register(owner, "profiles_locale", context -> value(context, Snapshot::locale));
        }
        if (economy != null) {
            registry.register(owner, "economy_balance", context -> value(context, snapshot -> Long.toString(snapshot.balanceMinor())));
            events.subscribe(owner, BalanceChanged.class, event -> refresh(event.playerId()));
            events.subscribe(owner, CrateOpened.class, event -> refresh(event.playerId()));
        }
        if (ranks != null) {
            registry.register(owner, "ranks_id", context -> value(context, Snapshot::rankId));
            events.subscribe(owner, RankChanged.class, event -> refresh(event.playerId()));
        }
        if (teams != null) {
            registry.register(owner, "teams_name", context -> value(context, Snapshot::teamName));
            events.subscribe(owner, TeamChanged.class, event -> refresh(event.actorId()));
        }
        if (rewards != null) {
            registry.register(owner, "rewards_daily_streak", context -> value(context, snapshot -> Integer.toString(snapshot.dailyStreak())));
            events.subscribe(owner, RewardClaimed.class, event -> refresh(event.playerId()));
        }
    }

    public CompletionStage<Void> refresh(UUID playerId) {
        var profile = profiles == null ? CompletableFuture.completedFuture(Optional.<com.magicstudios.magiccore.modules.profiles.PlayerProfile>empty())
                : profiles.find(playerId);
        var balance = economy == null ? CompletableFuture.completedFuture(new com.magicstudios.magiccore.modules.economy.Money("DISABLED", 0))
                : economy.balance(playerId, economy.primaryCurrency());
        var rank = ranks == null ? CompletableFuture.completedFuture("") : ranks.rankOf(playerId);
        var team = teams == null ? CompletableFuture.completedFuture(Optional.<com.magicstudios.magiccore.modules.teams.Team>empty())
                : teams.teamOf(playerId);
        var daily = rewards == null ? CompletableFuture.completedFuture(new com.magicstudios.magiccore.modules.rewards.DailyRewardState(playerId, null, 0, 0))
                : rewards.dailyState(playerId);
        return CompletableFuture.allOf(profile.toCompletableFuture(), balance.toCompletableFuture(),
                        rank.toCompletableFuture(), team.toCompletableFuture(), daily.toCompletableFuture())
                .thenRun(() -> cache.put(playerId, new Snapshot(
                        profile.toCompletableFuture().join().map(value -> value.currentName()).orElse(""),
                        profile.toCompletableFuture().join().map(value -> value.locale()).orElse("en_US"),
                        balance.toCompletableFuture().join().minorUnits(), rank.toCompletableFuture().join(),
                        team.toCompletableFuture().join().map(value -> value.name()).orElse(""),
                        daily.toCompletableFuture().join().currentStreak())));
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    private String value(PlaceholderContext context, java.util.function.Function<Snapshot, String> getter) {
        if (context.subjectId() == null) return "";
        Snapshot snapshot = cache.get(context.subjectId());
        return snapshot == null ? "" : getter.apply(snapshot);
    }

    private record Snapshot(String name, String locale, long balanceMinor, String rankId,
                            String teamName, int dailyStreak) { }
}
