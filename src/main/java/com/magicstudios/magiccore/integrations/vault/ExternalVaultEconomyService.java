package com.magicstudios.magiccore.integrations.vault;

import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyMutation;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.EconomyTransaction;
import com.magicstudios.magiccore.modules.economy.Money;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ExternalVaultEconomyService implements EconomyService {
    private final Economy provider;
    private final CurrencyDefinition currency;
    private final SchedulerFacade scheduler;
    private final TransactionalDataStore store;
    private final Clock clock;
    private final RecordRepository<VaultSaga> sagas = new RecordRepository<>("vault.saga", VaultSaga.class);
    private final RecordRepository<EconomyTransaction> ledger = new RecordRepository<>("economy.ledger", EconomyTransaction.class);

    public ExternalVaultEconomyService(Economy provider, CurrencyDefinition currency,
                                       SchedulerFacade scheduler, TransactionalDataStore store, Clock clock) {
        this.provider = provider;
        this.currency = currency;
        this.scheduler = scheduler;
        this.store = store;
        this.clock = clock;
    }

    @Override public String primaryCurrency() { return currency.id(); }
    @Override public Map<String, CurrencyDefinition> currencies() { return Map.of(currency.id(), currency); }

    @Override
    public CompletionStage<Money> balance(UUID playerId, String currencyId) {
        requireCurrency(currencyId);
        return global(() -> fromVault(provider.getBalance(Bukkit.getOfflinePlayer(playerId))));
    }

    @Override
    public CompletionStage<EconomyMutation> transfer(UUID from, UUID to, Money amount, String operationKey) {
        requireCurrency(amount.currency());
        if (amount.minorUnits() <= 0 || from.equals(to)) throw new IllegalArgumentException("Invalid transfer");
        return prepare(new VaultSaga(operationKey, "TRANSFER", from, to, amount.minorUnits(), "PREPARED", "",
                0, clock.instant())).thenCompose(prepared -> {
            if (!prepared) return replay(operationKey);
            return global(() -> transferOnProvider(from, to, amount)).thenCompose(outcome -> finalizeTransfer(operationKey, outcome));
        });
    }

    @Override
    public CompletionStage<EconomyMutation> adjust(UUID playerId, Money delta, String actor, String reason, String operationKey) {
        requireCurrency(delta.currency());
        return prepare(new VaultSaga(operationKey, "ADJUST", delta.minorUnits() < 0 ? playerId : null,
                delta.minorUnits() >= 0 ? playerId : null, Math.abs(delta.minorUnits()), "PREPARED", reason,
                0, clock.instant())).thenCompose(prepared -> {
            if (!prepared) return replay(operationKey);
            return global(() -> adjustOnProvider(playerId, delta)).thenCompose(outcome ->
                    finalizeAdjust(operationKey, playerId, delta, actor, reason, outcome));
        });
    }

    @Override
    public CompletionStage<List<EconomyTransaction>> transactions(String afterKey, int limit) {
        return store.read(reader -> ledger.scan(reader, afterKey, limit).stream()
                .map(RecordRepository.VersionedValue::value).toList());
    }

    private CompletionStage<Boolean> prepare(VaultSaga saga) {
        return store.transact("vault-saga-prepare:" + saga.operationKey(), transaction ->
                sagas.putIfAbsent(transaction, saga.operationKey(), saga));
    }

    private CompletionStage<EconomyMutation> replay(String operationKey) {
        return store.read(reader -> sagas.get(reader, operationKey).orElseThrow()).thenApply(existing -> {
            VaultSaga saga = existing.value();
            return new EconomyMutation(false, new Money(currency.id(), saga.resultingBalanceMinor()), null);
        });
    }

    private ProviderOutcome transferOnProvider(UUID from, UUID to, Money amount) {
        double decimal = amount.decimal(currency).doubleValue();
        var fromPlayer = Bukkit.getOfflinePlayer(from);
        var toPlayer = Bukkit.getOfflinePlayer(to);
        long fromBefore = fromVault(provider.getBalance(fromPlayer)).minorUnits();
        long toBefore = fromVault(provider.getBalance(toPlayer)).minorUnits();
        EconomyResponse withdrew = provider.withdrawPlayer(fromPlayer, decimal);
        if (!withdrew.transactionSuccess()) return new ProviderOutcome(false, "WITHDRAW_FAILED: " + withdrew.errorMessage,
                fromBefore, fromBefore, toBefore, toBefore, false);
        EconomyResponse deposited = provider.depositPlayer(toPlayer, decimal);
        if (!deposited.transactionSuccess()) {
            EconomyResponse compensation = provider.depositPlayer(fromPlayer, decimal);
            return new ProviderOutcome(false, "DEPOSIT_FAILED: " + deposited.errorMessage,
                    fromBefore, fromVault(provider.getBalance(fromPlayer)).minorUnits(), toBefore,
                    fromVault(provider.getBalance(toPlayer)).minorUnits(), !compensation.transactionSuccess());
        }
        return new ProviderOutcome(true, "", fromBefore, fromVault(provider.getBalance(fromPlayer)).minorUnits(),
                toBefore, fromVault(provider.getBalance(toPlayer)).minorUnits(), false);
    }

    private ProviderOutcome adjustOnProvider(UUID player, Money delta) {
        var offline = Bukkit.getOfflinePlayer(player);
        long before = fromVault(provider.getBalance(offline)).minorUnits();
        double decimal = BigDecimal.valueOf(Math.abs(delta.minorUnits()), currency.decimalPlaces()).doubleValue();
        EconomyResponse response = delta.minorUnits() >= 0 ? provider.depositPlayer(offline, decimal)
                : provider.withdrawPlayer(offline, decimal);
        long after = fromVault(provider.getBalance(offline)).minorUnits();
        return new ProviderOutcome(response.transactionSuccess(), response.errorMessage,
                delta.minorUnits() < 0 ? before : 0, delta.minorUnits() < 0 ? after : 0,
                delta.minorUnits() >= 0 ? before : 0, delta.minorUnits() >= 0 ? after : 0, false);
    }

    private CompletionStage<EconomyMutation> finalizeTransfer(String operationKey, ProviderOutcome outcome) {
        Instant now = clock.instant();
        return store.transact("vault-saga-finalize:" + operationKey, transaction -> {
            var current = sagas.get(transaction, operationKey).orElseThrow();
            String state = outcome.success ? "COMPLETED" : outcome.reconciliationRequired ? "RECONCILIATION_REQUIRED" : "FAILED";
            VaultSaga updated = new VaultSaga(operationKey, current.value().type(), current.value().fromPlayer(),
                    current.value().toPlayer(), current.value().amountMinor(), state, outcome.detail,
                    outcome.fromAfter, now);
            sagas.put(transaction, operationKey, updated, current.revision());
            if (!outcome.success) return new EconomyMutation(false, new Money(currency.id(), outcome.fromAfter), null);
            EconomyTransaction entry = new EconomyTransaction(UUID.randomUUID(), operationKey, "EXTERNAL_TRANSFER", currency.id(),
                    current.value().fromPlayer(), current.value().toPlayer(), current.value().amountMinor(),
                    outcome.fromBefore, outcome.fromAfter, outcome.toBefore, outcome.toAfter,
                    current.value().fromPlayer().toString(), "Vault external transfer", now);
            ledger.put(transaction, ledgerKey(entry), entry, 0);
            return new EconomyMutation(true, new Money(currency.id(), outcome.fromAfter), entry);
        });
    }

    private CompletionStage<EconomyMutation> finalizeAdjust(String operationKey, UUID player, Money delta,
                                                             String actor, String reason, ProviderOutcome outcome) {
        Instant now = clock.instant();
        return store.transact("vault-saga-finalize:" + operationKey, transaction -> {
            var current = sagas.get(transaction, operationKey).orElseThrow();
            long resulting = delta.minorUnits() < 0 ? outcome.fromAfter : outcome.toAfter;
            VaultSaga updated = new VaultSaga(operationKey, current.value().type(), current.value().fromPlayer(),
                    current.value().toPlayer(), current.value().amountMinor(), outcome.success ? "COMPLETED" : "FAILED",
                    outcome.detail, resulting, now);
            sagas.put(transaction, operationKey, updated, current.revision());
            if (!outcome.success) return new EconomyMutation(false, new Money(currency.id(), resulting), null);
            EconomyTransaction entry = new EconomyTransaction(UUID.randomUUID(), operationKey,
                    delta.minorUnits() >= 0 ? "EXTERNAL_ISSUANCE" : "EXTERNAL_SINK", currency.id(),
                    delta.minorUnits() < 0 ? player : null, delta.minorUnits() >= 0 ? player : null,
                    Math.abs(delta.minorUnits()), outcome.fromBefore, outcome.fromAfter, outcome.toBefore, outcome.toAfter,
                    actor, reason, now);
            ledger.put(transaction, ledgerKey(entry), entry, 0);
            return new EconomyMutation(true, new Money(currency.id(), resulting), entry);
        });
    }

    private Money fromVault(double amount) {
        return Money.exact(currency.id(), BigDecimal.valueOf(amount), currency);
    }

    private void requireCurrency(String id) {
        if (!currency.id().equals(id)) throw new IllegalArgumentException("External Vault mode exposes only " + currency.id());
    }

    private <T> CompletionStage<T> global(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.executeGlobal(() -> {
            try { future.complete(callable.call()); }
            catch (Throwable failure) { future.completeExceptionally(failure); }
        });
        return future;
    }

    private static String ledgerKey(EconomyTransaction entry) {
        return String.format("%013d:%s", entry.timestamp().toEpochMilli(), entry.id());
    }

    private record ProviderOutcome(boolean success, String detail, long fromBefore,
                                   long fromAfter, long toBefore, long toAfter,
                                   boolean reconciliationRequired) { }
}
