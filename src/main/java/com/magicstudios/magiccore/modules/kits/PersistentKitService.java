package com.magicstudios.magiccore.modules.kits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.delivery.DeliveryTransactionSupport;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PersistentKitService implements KitService {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final TransactionalDataStore store;
    private final CapabilityService capabilities;
    private final Clock clock;
    private final Map<String, KitDefinition> definitions;
    private final RecordRepository<KitClaim> claims = new RecordRepository<>("kits.claim", KitClaim.class);

    public PersistentKitService(TransactionalDataStore store, CapabilityService capabilities, Clock clock,
                                List<KitDefinition> definitions) {
        this.store = store;
        this.capabilities = capabilities;
        this.clock = clock;
        this.definitions = definitions.stream().collect(Collectors.toUnmodifiableMap(KitDefinition::id, Function.identity()));
    }

    @Override public List<KitDefinition> definitions() { return List.copyOf(definitions.values()); }

    @Override
    public CompletionStage<KitClaimResult> claim(UUID playerId, String kitId, String operationKey) {
        KitDefinition kit = definitions.get(kitId.toLowerCase(Locale.ROOT));
        if (kit == null) throw new IllegalArgumentException("KIT_NOT_FOUND");
        CompletionStage<Boolean> authorized = kit.capability() == null || kit.capability().isBlank()
                ? java.util.concurrent.CompletableFuture.completedFuture(true)
                : capabilities.has(playerId, kit.capability());
        return authorized.thenCompose(allowed -> {
            if (!allowed) throw new IllegalStateException("KIT_NOT_AUTHORIZED");
            return store.transact("kit-claim:" + operationKey, tx -> {
                String key = playerId + ":" + kit.id();
                var current = claims.get(tx, key);
                if (!IdempotencyKeys.reserve(tx, "kit-claim", operationKey)) {
                    return new KitClaimResult(false, "REPLAY", current.map(v -> v.value().nextAvailableAt()).orElse(null));
                }
                var now = clock.instant();
                if (current.isPresent() && current.get().value().nextAvailableAt().isAfter(now))
                    return new KitClaimResult(false, "COOLDOWN", current.get().value().nextAvailableAt());
                KitClaim claim = new KitClaim(playerId, kit.id(), now, now.plus(kit.cooldown()));
                claims.put(tx, key, claim, current.map(RecordRepository.VersionedValue::revision).orElse(0L));
                MailboxDelivery delivery = MailboxDelivery.pending(UUID.randomUUID(), playerId, operationKey,
                        "magiccore/kit-v1", JSON.writeValueAsBytes(kit.items()), now);
                DeliveryTransactionSupport.enqueue(tx, delivery);
                return new KitClaimResult(true, "CLAIMED", claim.nextAvailableAt());
            });
        });
    }
}
