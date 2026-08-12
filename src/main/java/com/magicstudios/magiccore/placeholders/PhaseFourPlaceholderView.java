package com.magicstudios.magiccore.placeholders;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.modules.combat.CombatService;
import com.magicstudios.magiccore.modules.combat.NewPlayerProtectionService;
import com.magicstudios.magiccore.modules.lifesteal.HeartAccount;
import com.magicstudios.magiccore.modules.lifesteal.HeartTransferred;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.lifesteal.PlayerEliminated;
import com.magicstudios.magiccore.modules.lifesteal.PlayerRevived;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/** Cached heart state plus lock-free combat views for synchronous placeholder consumers. */
public final class PhaseFourPlaceholderView {
    private final LifestealService lifesteal;
    private final CombatService combat;
    private final NewPlayerProtectionService newbies;
    private final Map<UUID, HeartAccount> hearts = new ConcurrentHashMap<>();

    public PhaseFourPlaceholderView(LifestealService lifesteal, CombatService combat,
                                    NewPlayerProtectionService newbies) {
        this.lifesteal = lifesteal;
        this.combat = combat;
        this.newbies = newbies;
    }

    public void register(String owner, PlaceholderRegistry registry, DomainEventBus events) {
        registry.register(owner, "lifesteal_hearts", context -> heartValue(context, account -> Integer.toString(account.hearts())));
        registry.register(owner, "lifesteal_eliminated", context -> heartValue(context, account -> Boolean.toString(account.eliminated())));
        registry.register(owner, "combat_tagged", context -> subject(context) == null ? ""
                : Boolean.toString(combat.isTagged(subject(context))));
        registry.register(owner, "combat_remaining_seconds", context -> subject(context) == null ? ""
                : Long.toString(Math.max(0, combat.remaining(subject(context)).toSeconds())));
        registry.register(owner, "combat_newbie_protected", context -> subject(context) == null ? ""
                : Boolean.toString(newbies.state(subject(context)) == NewPlayerProtectionService.State.PROTECTED));
        events.subscribe(owner, HeartTransferred.class, event -> {
            refresh(event.killerId());
            refresh(event.victimId());
        });
        events.subscribe(owner, PlayerEliminated.class, event -> refresh(event.playerId()));
        events.subscribe(owner, PlayerRevived.class, event -> refresh(event.playerId()));
    }

    public CompletionStage<Void> refresh(UUID playerId) {
        return lifesteal.account(playerId).thenAccept(account -> hearts.put(playerId, account));
    }

    public void invalidate(UUID playerId) {
        hearts.remove(playerId);
    }

    private String heartValue(PlaceholderContext context, java.util.function.Function<HeartAccount, String> getter) {
        UUID id = subject(context);
        HeartAccount account = id == null ? null : hearts.get(id);
        return account == null ? "" : getter.apply(account);
    }

    private static UUID subject(PlaceholderContext context) {
        return context.subjectId();
    }
}
