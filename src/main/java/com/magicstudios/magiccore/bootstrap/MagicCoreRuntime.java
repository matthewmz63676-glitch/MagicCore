package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.api.ServiceRegistry;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.audit.PersistentAuditService;
import com.magicstudios.magiccore.commands.CommandRegistry;
import com.magicstudios.magiccore.config.MagicCoreConfiguration;
import com.magicstudios.magiccore.config.MagicCoreConfigurationLoader;
import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.ValidationIssue;
import com.magicstudios.magiccore.config.YamlConfigCodec;
import com.magicstudios.magiccore.config.model.FeaturesFile;
import com.magicstudios.magiccore.delivery.DeliveryMailbox;
import com.magicstudios.magiccore.delivery.PersistentDeliveryMailbox;
import com.magicstudios.magiccore.diagnostics.DiagnosticsRegistry;
import com.magicstudios.magiccore.modules.ModuleContext;
import com.magicstudios.magiccore.modules.ModuleManager;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.storage.MariaDbDataStoreFactory;
import com.magicstudios.magiccore.storage.MigrationRunner;
import com.magicstudios.magiccore.storage.MongoTransactionalDataStore;
import com.magicstudios.magiccore.storage.SqliteDataStoreFactory;
import com.magicstudios.magiccore.storage.StorageMigration;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class MagicCoreRuntime implements AutoCloseable {
    private final Plugin plugin;
    private final SchedulerFacade scheduler;
    private final ServiceRegistry services;
    private final DomainEventBus events;
    private final CommandRegistry commands;
    private final PlaceholderRegistry placeholders;
    private final DiagnosticsRegistry diagnostics;
    private final Clock clock;
    private final AtomicReference<MagicCoreConfiguration> configuration = new AtomicReference<>();
    private volatile TransactionalDataStore store;
    private volatile ModuleManager modules;
    private volatile BoundedIoExecutor configExecutor;
    private volatile AtomicConfigStore<FeaturesFile> featuresStore;

    public MagicCoreRuntime(Plugin plugin, SchedulerFacade scheduler, ServiceRegistry services,
                            DomainEventBus events, CommandRegistry commands,
                            PlaceholderRegistry placeholders, DiagnosticsRegistry diagnostics,
                            Clock clock) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.services = services;
        this.events = events;
        this.commands = commands;
        this.placeholders = placeholders;
        this.diagnostics = diagnostics;
        this.clock = clock;
    }

    public CompletionStage<Void> start() {
        Path dataDirectory = plugin.getDataFolder().toPath();
        MagicCoreConfigurationLoader loader = new MagicCoreConfigurationLoader(dataDirectory, plugin::getResource);
        return scheduler.supplyAsync(() -> loader.installAndLoad()).thenCompose(loaded -> {
            configuration.set(loaded);
            configExecutor = new BoundedIoExecutor(1, 128, "magiccore-config");
            featuresStore = new AtomicConfigStore<>(dataDirectory.resolve("features.yml"), dataDirectory.resolve("backups"),
                    new YamlConfigCodec<>(FeaturesFile.class), MagicCoreRuntime::validateFeatures, configExecutor);
            try {
                store = createStore(dataDirectory, loaded);
            } catch (Exception failure) {
                return CompletableFuture.failedFuture(failure);
            }
            return featuresStore.load().thenCompose(ignored -> store.start())
                    .thenCompose(ignored -> migrate()).thenCompose(ignored -> runGlobal(() -> initialize(loaded)));
        });
    }

    public CompletionStage<ReloadResult> reload() {
        MagicCoreConfigurationLoader loader = new MagicCoreConfigurationLoader(plugin.getDataFolder().toPath(), plugin::getResource);
        return scheduler.supplyAsync(loader::installAndLoad).thenApply(candidate -> {
            MagicCoreConfiguration current = configuration.get();
            List<String> changes = new ArrayList<>();
            if (!current.core().equals(candidate.core())) changes.add("config.yml");
            if (!current.features().equals(candidate.features())) changes.add("features.yml");
            if (!current.integrations().equals(candidate.integrations())) changes.add("integrations.yml");
            if (!current.storage().equals(candidate.storage())) changes.add("storage.yml");
            if (!current.ranks().equals(candidate.ranks())) changes.add("ranks.yml");
            if (!current.economy().equals(candidate.economy())) changes.add("modules/economy.yml");
            if (!current.teams().equals(candidate.teams())) changes.add("modules/teams.yml");
            if (!current.rewards().equals(candidate.rewards())) changes.add("modules/rewards.yml");
            if (!current.essentials().equals(candidate.essentials())) changes.add("modules/essentials.yml");
            if (!current.shop().equals(candidate.shop())) changes.add("modules/shop.yml");
            if (!current.settings().equals(candidate.settings())) changes.add("modules/settings.yml");
            if (!current.auction().equals(candidate.auction())) changes.add("modules/auction.yml");
            if (!current.orders().equals(candidate.orders())) changes.add("modules/orders.yml");
            if (!current.bounties().equals(candidate.bounties())) changes.add("modules/bounties.yml");
            if (!current.lifesteal().equals(candidate.lifesteal())) changes.add("modules/lifesteal.yml");
            if (!current.combat().equals(candidate.combat())) changes.add("modules/combat.yml");
            if (!current.crates().equals(candidate.crates())) changes.add("modules/crates.yml");
            if (!current.display().equals(candidate.display())) changes.add("modules/display.yml");
            if (!current.store().equals(candidate.store())) changes.add("modules/store.yml");
            if (!current.afk().equals(candidate.afk())) changes.add("modules/afk.yml");
            if (!current.presentation().equals(candidate.presentation())) changes.add("modules/presentation.yml");
            if (!current.spawnStash().equals(candidate.spawnStash())) changes.add("modules/spawnstash.yml");
            if (!current.itemWorth().equals(candidate.itemWorth())) changes.add("modules/item-worth.yml");
            if (!current.billford().equals(candidate.billford())) changes.add("modules/billford.yml");
            if (!current.tools().equals(candidate.tools())) changes.add("modules/tools.yml");
            if (!current.secureStorage().equals(candidate.secureStorage())) changes.add("modules/secure-storage.yml");
            if (!current.discordBridge().equals(candidate.discordBridge())) changes.add("modules/discord-bridge.yml");
            if (!current.playerWarps().equals(candidate.playerWarps())) changes.add("modules/playerwarps.yml");
            if (!current.menus().equals(candidate.menus())) changes.add("modules/menus.yml");
            if (!current.events().equals(candidate.events())) changes.add("modules/events.yml");
            if (!current.messages().equals(candidate.messages())) changes.add("messages.yml");
            boolean restart = changes.stream().anyMatch(file -> !file.equals("messages.yml"));
            if (!restart) configuration.set(candidate);
            return new ReloadResult(!restart, restart, changes);
        });
    }

    public ServiceRegistry services() {
        return services;
    }

    public DiagnosticsRegistry diagnostics() {
        return diagnostics;
    }

    public boolean ready() {
        return modules != null;
    }

    public MagicCoreConfiguration configuration() {
        return configuration.get();
    }

    public Path importsDirectory(){return plugin.getDataFolder().toPath().resolve("imports").toAbsolutePath().normalize();}

    private void initialize(MagicCoreConfiguration loaded) {
        services.register("foundation-storage", TransactionalDataStore.class, store);
        AuditService audit = new PersistentAuditService(store);
        services.register("foundation-audit", AuditService.class, audit);
        services.register("foundation-delivery", DeliveryMailbox.class, new PersistentDeliveryMailbox(store, clock));
        diagnostics.register("foundation-storage", "storage", store::health);
        diagnostics.register("foundation-config", "configuration-warnings", () -> {
            List<String> warnings = com.magicstudios.magiccore.config.ConfigurationWarnings.collect(loaded);
            return new com.magicstudios.magiccore.api.HealthReport("configuration-warnings",
                    warnings.isEmpty() ? com.magicstudios.magiccore.api.HealthState.HEALTHY : com.magicstudios.magiccore.api.HealthState.DEGRADED,
                    warnings.isEmpty() ? "no configuration warnings" : String.join("; ", warnings),
                    java.util.Map.of("count", Integer.toString(warnings.size())), clock.instant());
        });
        var compatibility=com.magicstudios.magiccore.diagnostics.CompatibilitySnapshot.capture(plugin,clock);
        diagnostics.register("foundation-compatibility","compatibility",()->compatibility);
        ModuleContext context = new ModuleContext(services, events, commands, placeholders, diagnostics);
        modules = new ModuleManager(context);
        modules.discover(new PhaseOneModuleFactory(plugin, loaded, store, clock, featuresStore, scheduler).create());
        modules.startAll();
        diagnostics.register("foundation-modules", "modules", () -> new com.magicstudios.magiccore.api.HealthReport(
                "modules", com.magicstudios.magiccore.api.HealthState.HEALTHY, modules.states().toString(),
                java.util.Map.of("states", modules.states().toString()), clock.instant()));
    }

    private CompletionStage<java.util.Map<String, Integer>> migrate() {
        List<StorageMigration> migrations = List.of("foundation", "profiles", "economy", "ranks", "teams", "rewards", "audit", "delivery",
                        "settings", "essentials", "kits", "shop", "playerwarps", "auction", "orders", "bounties", "statistics", "lifesteal", "combat", "crates", "display", "store", "imports", "leaderboards", "afk-shards", "presentation", "spawnstash", "item-worth", "billford", "tools", "secure-storage", "resets", "keyalls", "gemshop", "discord-bridge", "koth", "vote-party")
                .stream().map(module -> new StorageMigration(module, 1, "Initialize " + module + " namespace", tx -> null)).toList();
        return new MigrationRunner(store).migrate(migrations);
    }

    private TransactionalDataStore createStore(Path dataDirectory, MagicCoreConfiguration loaded) throws Exception {
        String provider = loaded.storage().provider().toUpperCase(Locale.ROOT);
        BoundedIoExecutor executor = new BoundedIoExecutor(loaded.core().io().threads(),
                loaded.core().io().queueCapacity(), "magiccore-storage");
        return switch (provider) {
            case "SQLITE" -> {
                Path file = dataDirectory.resolve(loaded.storage().sqlite().file()).normalize();
                if (!file.startsWith(dataDirectory.normalize())) throw new IllegalArgumentException("storage.yml sqlite.file must stay inside the MagicCore directory");
                Files.createDirectories(file.getParent());
                yield SqliteDataStoreFactory.create(file, executor);
            }
            case "MARIADB" -> MariaDbDataStoreFactory.create(loaded.storage().mariadb().host(),
                    loaded.storage().mariadb().port(), loaded.storage().mariadb().database(),
                    loaded.storage().mariadb().username(), resolveSecret(loaded.storage().mariadb().passwordFrom()), executor);
            case "MONGODB" -> new MongoTransactionalDataStore(resolveSecret(loaded.storage().mongodb().connectionStringFrom()),
                    loaded.storage().mongodb().database(), loaded.storage().mongodb().requireTransactions(), executor);
            default -> throw new IllegalArgumentException("storage.yml provider must be SQLITE, MARIADB, or MONGODB");
        };
    }

    private static String resolveSecret(String reference) {
        Objects.requireNonNull(reference, "secret reference");
        if (!reference.startsWith("env:")) throw new IllegalArgumentException("Secrets must use env:VARIABLE references");
        String variable = reference.substring(4);
        String value = System.getenv(variable);
        if (value == null || value.isBlank()) throw new IllegalStateException("Required environment variable is missing: " + variable);
        return value;
    }

    private static List<ValidationIssue> validateFeatures(FeaturesFile candidate) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (candidate.configVersion() != 1) {
            issues.add(ValidationIssue.error("features.yml:config-version", "unsupported schema version",
                    "set config-version to 1"));
        }
        boolean profilesDisabled = candidate.features().getOrDefault("profiles", com.magicstudios.magiccore.api.ProviderMode.DISABLED)
                == com.magicstudios.magiccore.api.ProviderMode.DISABLED;
        if (profilesDisabled && candidate.features().entrySet().stream()
                .anyMatch(entry -> Set.of("economy", "ranks", "teams", "rewards").contains(entry.getKey())
                        && entry.getValue() != com.magicstudios.magiccore.api.ProviderMode.DISABLED)) {
            issues.add(ValidationIssue.error("features.yml:features.profiles",
                    "profiles cannot be disabled while a Phase 1 dependent module is enabled",
                    "enable profiles or disable economy, ranks, teams, and rewards"));
        }
        if (candidate.features().getOrDefault("teams", com.magicstudios.magiccore.api.ProviderMode.DISABLED)
                != com.magicstudios.magiccore.api.ProviderMode.DISABLED
                && candidate.features().getOrDefault("ranks", com.magicstudios.magiccore.api.ProviderMode.DISABLED)
                == com.magicstudios.magiccore.api.ProviderMode.DISABLED) {
            issues.add(ValidationIssue.error("features.yml:features.teams", "teams requires ranks for TEAM_SIZE limits",
                    "enable ranks or disable teams"));
        }
        if (candidate.features().getOrDefault("rewards", com.magicstudios.magiccore.api.ProviderMode.DISABLED)
                != com.magicstudios.magiccore.api.ProviderMode.DISABLED
                && candidate.features().getOrDefault("economy", com.magicstudios.magiccore.api.ProviderMode.DISABLED)
                == com.magicstudios.magiccore.api.ProviderMode.DISABLED) {
            issues.add(ValidationIssue.error("features.yml:features.rewards", "configured rewards require the economy service",
                    "enable economy or disable rewards"));
        }
        return List.copyOf(issues);
    }

    private CompletionStage<Void> runGlobal(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.executeGlobal(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    @Override
    public void close() {
        if (modules != null) modules.stopAll();
        if (store != null) store.close();
        if (configExecutor != null) configExecutor.close();
    }
}
