package com.magicstudios.magiccore.integrations.vault;

import com.magicstudios.magiccore.modules.economy.CurrencyDefinition;
import com.magicstudios.magiccore.modules.economy.EconomyMutation;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.Money;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class MagicCoreVaultEconomy extends AbstractEconomy {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final EconomyService economy;
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;

    public MagicCoreVaultEconomy(EconomyService economy) {
        this.economy = economy;
    }

    public void disable() {
        enabled = false;
    }

    public void cache(UUID playerId, long minorUnits) {
        balanceCache.put(playerId, new Money(economy.primaryCurrency(), minorUnits).decimal(definition()).doubleValue());
    }

    @Override public boolean isEnabled() { return enabled; }
    @Override public String getName() { return "MagicCore"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return definition().decimalPlaces(); }
    @Override public String format(double amount) { return definition().symbol() + BigDecimal.valueOf(amount).setScale(fractionalDigits()); }
    @Override public String currencyNamePlural() { return definition().display(); }
    @Override public String currencyNameSingular() { return definition().display(); }
    @Override public boolean hasAccount(String playerName) { return resolve(playerName) != null; }
    @Override public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }

    @Override
    public double getBalance(String playerName) {
        UUID player = resolve(playerName);
        if (player == null) return 0;
        if (VaultThreadGuard.isTickThread()) return balanceCache.getOrDefault(player, 0D);
        try {
            Money balance = economy.balance(player, economy.primaryCurrency()).toCompletableFuture()
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            double value = balance.decimal(definition()).doubleValue();
            balanceCache.put(player, value);
            return value;
        } catch (Exception failure) {
            return 0;
        }
    }

    @Override public double getBalance(String playerName, String world) { return getBalance(playerName); }
    @Override public boolean has(String playerName, double amount) { return getBalance(playerName) >= amount; }
    @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return mutate(playerName, -amount, "vault-withdraw"); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return mutate(playerName, amount, "vault-deposit"); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public EconomyResponse createBank(String name, String player) { return unsupported(); }
    @Override public EconomyResponse deleteBank(String name) { return unsupported(); }
    @Override public EconomyResponse bankBalance(String name) { return unsupported(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return unsupported(); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return unsupported(); }
    @Override public List<String> getBanks() { return List.of(); }
    @Override public boolean createPlayerAccount(String playerName) { return hasAccount(playerName); }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return createPlayerAccount(playerName); }

    private EconomyResponse mutate(String playerName, double amount, String reason) {
        UUID player = resolve(playerName);
        if (player == null) return failure(0, "Unknown player profile");
        if (VaultThreadGuard.isTickThread()) {
            return failure(getBalance(playerName), "Synchronous Vault mutation refused on a server tick thread; use MagicCore's async EconomyService");
        }
        try {
            Money delta = Money.exact(economy.primaryCurrency(), BigDecimal.valueOf(amount), definition());
            EconomyMutation result = economy.adjust(player, delta, "Vault", reason, "vault:" + UUID.randomUUID())
                    .toCompletableFuture().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new EconomyResponse(Math.abs(amount), result.resultingBalance().decimal(definition()).doubleValue(),
                    EconomyResponse.ResponseType.SUCCESS, "");
        } catch (Exception failure) {
            return failure(getBalance(playerName), failure.getMessage());
        }
    }

    private CurrencyDefinition definition() {
        return economy.currencies().get(economy.primaryCurrency());
    }

    private static UUID resolve(String playerName) {
        if (playerName == null || playerName.isBlank()) return null;
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    private static EconomyResponse unsupported() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "MagicCore does not support Vault banks");
    }

    private static EconomyResponse failure(double balance, String message) {
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, message == null ? "Vault operation failed" : message);
    }
}
