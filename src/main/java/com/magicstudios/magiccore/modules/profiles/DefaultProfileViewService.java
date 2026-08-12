package com.magicstudios.magiccore.modules.profiles;

import com.magicstudios.magiccore.audit.AuditEvent;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.ranks.RankService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class DefaultProfileViewService implements ProfileViewService {
    private static final String ADMIN_CAPABILITY = "PROFILE_ADMIN_VIEW";
    private final PlayerProfileService profiles;
    private final PlayerSettingsService settings;
    private final PlayerStatsService stats;
    private final RankService ranks;
    private final ShardService shards;
    private final CapabilityService capabilities;
    private final AuditService audit;
    private final EconomyService economy;

    public DefaultProfileViewService(PlayerProfileService profiles, PlayerSettingsService settings,
                                     PlayerStatsService stats, RankService ranks, ShardService shards,
                                     CapabilityService capabilities, AuditService audit, EconomyService economy) {
        this.profiles = profiles; this.settings = settings; this.stats = stats; this.ranks = ranks;
        this.shards = shards; this.capabilities = capabilities; this.audit = audit; this.economy = economy;
    }

    @Override
    public CompletionStage<ProfileView> view(UUID viewerId, UUID targetId) {
        return settings.get(targetId).thenCompose(targetSettings -> {
            boolean owner = viewerId.equals(targetId);
            boolean isPublic = targetSettings.enabled(PlayerSetting.PROFILE_PUBLIC);
            CompletionStage<Boolean> adminCheck = owner || isPublic ? CompletableFuture.completedFuture(false)
                    : capabilities.has(viewerId, ADMIN_CAPABILITY);
            return adminCheck.thenCompose(admin -> {
                if (!owner && !isPublic && !admin) return CompletableFuture.completedFuture(ProfileView.denied(targetId));
                return build(targetId, admin, targetSettings.values());
            });
        });
    }

    @Override
    public CompletionStage<ProfileView> administrativeView(UUID viewerId, UUID targetId) {
        return capabilities.has(viewerId, ADMIN_CAPABILITY).thenCompose(allowed -> {
            if (!allowed) return CompletableFuture.completedFuture(ProfileView.denied(targetId));
            return settings.get(targetId).thenCompose(targetSettings -> build(targetId, true, targetSettings.values()));
        });
    }

    private CompletionStage<ProfileView> build(UUID targetId, boolean administrative,
                                               java.util.Map<PlayerSetting, Boolean> targetSettings) {
        var profile = profiles.find(targetId).toCompletableFuture();
        var playerStats = stats.stats(targetId).toCompletableFuture();
        var rank = ranks.rankOf(targetId).toCompletableFuture();
        var shardBalance = shards.balance(targetId).toCompletableFuture();
        CompletableFuture<List<AuditEvent>> audits = administrative ? audit.recent(null, 1000).toCompletableFuture()
                : CompletableFuture.completedFuture(List.of());
        var transactions = administrative ? economy.transactions(null, 1000).toCompletableFuture()
                : CompletableFuture.completedFuture(List.<com.magicstudios.magiccore.modules.economy.EconomyTransaction>of());
        return CompletableFuture.allOf(profile, playerStats, rank, shardBalance, audits, transactions).thenApply(ignored -> {
            Optional<PlayerProfile> found = profile.join();
            if (found.isEmpty()) return ProfileView.denied(targetId);
            PlayerProfile p = found.orElseThrow();
            var s = playerStats.join();
            String target = targetId.toString();
            List<UUID> auditIds = audits.join().stream().filter(event -> event.target().equals(target)).limit(20).map(AuditEvent::id).toList();
            List<UUID> transactionIds = transactions.join().stream()
                    .filter(tx -> targetId.equals(tx.fromPlayer()) || targetId.equals(tx.toPlayer())).limit(20)
                    .map(com.magicstudios.magiccore.modules.economy.EconomyTransaction::id).toList();
            return new ProfileView(targetId, true, administrative, "", p.currentName(), rank.join(), p.firstSeen(), p.lastSeen(),
                    s.kills(), s.deaths(), s.playtimeSeconds(), shardBalance.join().amount(),
                    administrative ? targetSettings : java.util.Map.of(), auditIds, transactionIds);
        });
    }
}
