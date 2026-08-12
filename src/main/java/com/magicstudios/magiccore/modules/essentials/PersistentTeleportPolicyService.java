package com.magicstudios.magiccore.modules.essentials;

import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyTransactionSupport;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentTeleportPolicyService implements TeleportPolicyService {
    private final TransactionalDataStore store;
    private final CurrencyDefinition currency;
    private final long costMinor;
    private final Duration cooldown;
    private final Clock clock;
    private final RecordRepository<TeleportPermit> permits = new RecordRepository<>("essentials.teleport.permit", TeleportPermit.class);
    private final RecordRepository<TeleportPolicyState> states = new RecordRepository<>("essentials.teleport.state", TeleportPolicyState.class);

    public PersistentTeleportPolicyService(TransactionalDataStore store, CurrencyDefinition currency,
                                           long costMinor, Duration cooldown, Clock clock) {
        if (costMinor < 0) throw new IllegalArgumentException("Teleport cost cannot be negative");
        if (cooldown.isNegative()) throw new IllegalArgumentException("Teleport cooldown cannot be negative");
        this.store = store; this.currency = currency; this.costMinor = costMinor; this.cooldown = cooldown; this.clock = clock;
    }

    @Override public CompletionStage<TeleportPermit> reserve(UUID playerId, String operationKey) {
        return store.transact("teleport-reserve:" + operationKey, tx -> {
            var stateRecord = states.get(tx, playerId.toString());
            TeleportPolicyState state = stateRecord.map(RecordRepository.VersionedValue::value)
                    .orElse(new TeleportPolicyState(playerId, null, Instant.EPOCH, clock.instant()));
            if (!IdempotencyKeys.reserve(tx, "teleport-reserve", operationKey))
                throw new IllegalStateException("TELEPORT_OPERATION_REPLAY");
            if (state.activePermitId() != null) throw new IllegalStateException("TELEPORT_ALREADY_ACTIVE");
            if (state.cooldownUntil().isAfter(clock.instant()))
                throw new IllegalStateException("TELEPORT_COOLDOWN_UNTIL:" + state.cooldownUntil());
            if (costMinor > 0) EconomyTransactionSupport.credit(tx, playerId, currency, -costMinor,
                    operationKey + ":debit", playerId.toString(), "teleport-cost", clock.instant());
            TeleportPermit permit = new TeleportPermit(UUID.randomUUID(), playerId, operationKey, costMinor,
                    clock.instant(), state.cooldownUntil(), TeleportPermit.Status.RESERVED);
            permits.put(tx, permit.id().toString(), permit, 0);
            states.put(tx, playerId.toString(), new TeleportPolicyState(playerId, permit.id(),
                    state.cooldownUntil(), clock.instant()), stateRecord.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return permit;
        });
    }

    @Override public CompletionStage<Boolean> complete(TeleportPermit permit) {
        return finish(permit, true, "completed");
    }

    @Override public CompletionStage<Boolean> refund(TeleportPermit permit, String reason) {
        return finish(permit, false, reason);
    }

    private CompletionStage<Boolean> finish(TeleportPermit permit, boolean completed, String reason) {
        return store.transact("teleport-finish:" + permit.id(), tx -> {
            var current = permits.get(tx, permit.id().toString()).orElseThrow(() -> new IllegalStateException("TELEPORT_PERMIT_NOT_FOUND"));
            if (current.value().status() != TeleportPermit.Status.RESERVED) return false;
            var stateRecord = states.get(tx, permit.playerId().toString()).orElseThrow();
            if (!permit.id().equals(stateRecord.value().activePermitId())) throw new IllegalStateException("TELEPORT_PERMIT_STATE_CONFLICT");
            if (!completed && permit.costMinor() > 0) EconomyTransactionSupport.credit(tx, permit.playerId(), currency,
                    permit.costMinor(), permit.operationKey() + ":refund", "magiccore", "teleport-refund:" + reason, clock.instant());
            TeleportPermit updated = new TeleportPermit(permit.id(), permit.playerId(), permit.operationKey(),
                    permit.costMinor(), permit.reservedAt(), permit.previousCooldownUntil(),
                    completed ? TeleportPermit.Status.COMPLETED : TeleportPermit.Status.REFUNDED);
            permits.put(tx, permit.id().toString(), updated, current.revision());
            Instant until = completed ? clock.instant().plus(cooldown) : permit.previousCooldownUntil();
            states.put(tx, permit.playerId().toString(), new TeleportPolicyState(permit.playerId(), null, until,
                    clock.instant()), stateRecord.revision());
            return true;
        });
    }
}
