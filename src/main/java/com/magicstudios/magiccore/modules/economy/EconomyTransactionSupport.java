package com.magicstudios.magiccore.modules.economy;

import com.magicstudios.magiccore.storage.DataTransaction;
import com.magicstudios.magiccore.storage.RecordRepository;

import java.time.Instant;
import java.util.UUID;

public final class EconomyTransactionSupport {
    private static final RecordRepository<WalletBalance> BALANCES =
            new RecordRepository<>("economy.balance", WalletBalance.class);
    private static final RecordRepository<EconomyTransaction> LEDGER =
            new RecordRepository<>("economy.ledger", EconomyTransaction.class);

    private EconomyTransactionSupport() {
    }

    public static WalletBalance balance(com.magicstudios.magiccore.storage.DataReader reader, UUID playerId,
                                        CurrencyDefinition definition) throws Exception {
        return BALANCES.get(reader, key(playerId, definition.id())).map(RecordRepository.VersionedValue::value)
                .orElse(new WalletBalance(playerId, definition.id(), definition.startingBalanceMinor()));
    }

    public static AppliedBalance credit(DataTransaction transaction, UUID playerId, CurrencyDefinition definition,
                                        long deltaMinor, String operationKey, String actor, String reason,
                                        Instant now) throws Exception {
        String balanceKey = key(playerId, definition.id());
        var existing = BALANCES.get(transaction, balanceKey);
        long before = existing.map(value -> value.value().minorUnits()).orElse(definition.startingBalanceMinor());
        long after = Math.addExact(before, deltaMinor);
        if (after < 0) throw new IllegalStateException("INSUFFICIENT_FUNDS");
        if (after > definition.maximumBalanceMinor()) throw new IllegalStateException("MAXIMUM_BALANCE_EXCEEDED");
        WalletBalance updated = new WalletBalance(playerId, definition.id(), after);
        BALANCES.put(transaction, balanceKey, updated, existing.map(RecordRepository.VersionedValue::revision).orElse(0L));
        EconomyTransaction ledger = new EconomyTransaction(UUID.randomUUID(), operationKey,
                deltaMinor >= 0 ? "ISSUANCE" : "SINK", definition.id(), deltaMinor < 0 ? playerId : null,
                deltaMinor >= 0 ? playerId : null, Math.abs(deltaMinor),
                deltaMinor < 0 ? before : 0, deltaMinor < 0 ? after : 0,
                deltaMinor >= 0 ? before : 0, deltaMinor >= 0 ? after : 0,
                actor, reason, now);
        appendLedger(transaction, ledger);
        return new AppliedBalance(before, after, ledger);
    }

    public static EconomyTransaction transfer(DataTransaction transaction, UUID from, UUID to,
                                              CurrencyDefinition definition, long amountMinor,
                                              String operationKey, Instant now) throws Exception {
        if (from.equals(to)) throw new IllegalArgumentException("Cannot pay the same player");
        if (amountMinor <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        String fromKey = key(from, definition.id());
        String toKey = key(to, definition.id());
        var fromRecord = BALANCES.get(transaction, fromKey);
        var toRecord = BALANCES.get(transaction, toKey);
        long fromBefore = fromRecord.map(value -> value.value().minorUnits()).orElse(definition.startingBalanceMinor());
        long toBefore = toRecord.map(value -> value.value().minorUnits()).orElse(definition.startingBalanceMinor());
        long fromAfter = Math.subtractExact(fromBefore, amountMinor);
        long toAfter = Math.addExact(toBefore, amountMinor);
        if (fromAfter < 0) throw new IllegalStateException("INSUFFICIENT_FUNDS");
        if (toAfter > definition.maximumBalanceMinor()) throw new IllegalStateException("MAXIMUM_BALANCE_EXCEEDED");
        BALANCES.put(transaction, fromKey, new WalletBalance(from, definition.id(), fromAfter),
                fromRecord.map(RecordRepository.VersionedValue::revision).orElse(0L));
        BALANCES.put(transaction, toKey, new WalletBalance(to, definition.id(), toAfter),
                toRecord.map(RecordRepository.VersionedValue::revision).orElse(0L));
        EconomyTransaction ledger = new EconomyTransaction(UUID.randomUUID(), operationKey, "TRANSFER",
                definition.id(), from, to, amountMinor, fromBefore, fromAfter, toBefore, toAfter,
                from.toString(), "player-payment", now);
        appendLedger(transaction, ledger);
        return ledger;
    }

    private static void appendLedger(DataTransaction transaction, EconomyTransaction entry) throws Exception {
        String key = String.format("%013d:%s", entry.timestamp().toEpochMilli(), entry.id());
        LEDGER.put(transaction, key, entry, 0);
    }

    private static String key(UUID playerId, String currency) {
        return currency + ":" + playerId;
    }

    public record AppliedBalance(long beforeMinor, long afterMinor, EconomyTransaction transaction) {
    }
}
