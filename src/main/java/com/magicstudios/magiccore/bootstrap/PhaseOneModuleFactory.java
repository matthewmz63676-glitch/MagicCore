package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.admin.DefaultSetupService;
import com.magicstudios.magiccore.admin.InputSessionService;
import com.magicstudios.magiccore.admin.NativeInputSessionService;
import com.magicstudios.magiccore.admin.SetupService;
import com.magicstudios.magiccore.admin.AdminEditingService;
import com.magicstudios.magiccore.admin.DefaultAdminEditingService;
import com.magicstudios.magiccore.api.HealthReport;
import com.magicstudios.magiccore.api.HealthState;
import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.capabilities.RankCapabilityService;
import com.magicstudios.magiccore.config.MagicCoreConfiguration;
import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.model.FeaturesFile;
import com.magicstudios.magiccore.integrations.LuckPermsIntegrationBridge;
import com.magicstudios.magiccore.integrations.PlaceholderApiIntegrationBridge;
import com.magicstudios.magiccore.integrations.VaultIntegrationBridge;
import com.magicstudios.magiccore.integrations.WorldGuardIntegrationBridge;
import com.magicstudios.magiccore.integrations.ClaimsIntegrationBridge;
import com.magicstudios.magiccore.modules.MagicModule;
import com.magicstudios.magiccore.modules.ModuleContext;
import com.magicstudios.magiccore.modules.ModuleDescriptor;
import com.magicstudios.magiccore.modules.SimpleModule;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.economy.PersistentEconomyService;
import com.magicstudios.magiccore.modules.profiles.PersistentPlayerProfileService;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import com.magicstudios.magiccore.modules.rewards.PersistentRewardService;
import com.magicstudios.magiccore.modules.rewards.RewardService;
import com.magicstudios.magiccore.modules.teams.PersistentTeamService;
import com.magicstudios.magiccore.modules.teams.TeamNamePolicy;
import com.magicstudios.magiccore.modules.teams.TeamService;
import com.magicstudios.magiccore.modules.essentials.BackService;
import com.magicstudios.magiccore.modules.essentials.HomeService;
import com.magicstudios.magiccore.modules.essentials.PersistentBackService;
import com.magicstudios.magiccore.modules.essentials.PersistentHomeService;
import com.magicstudios.magiccore.modules.essentials.PersistentWarpService;
import com.magicstudios.magiccore.modules.essentials.TeleportRequestService;
import com.magicstudios.magiccore.modules.essentials.TeleportWarmupService;
import com.magicstudios.magiccore.modules.essentials.TeleportService;
import com.magicstudios.magiccore.modules.essentials.TeleportPolicyService;
import com.magicstudios.magiccore.modules.essentials.PersistentTeleportPolicyService;
import com.magicstudios.magiccore.modules.essentials.RtpService;
import com.magicstudios.magiccore.modules.essentials.RtpBounds;
import com.magicstudios.magiccore.modules.essentials.WarpService;
import com.magicstudios.magiccore.modules.kits.KitDefinition;
import com.magicstudios.magiccore.modules.kits.KitService;
import com.magicstudios.magiccore.modules.kits.PersistentKitService;
import com.magicstudios.magiccore.modules.settings.PersistentPlayerSettingsService;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.modules.shop.InternalShopService;
import com.magicstudios.magiccore.modules.shop.ShopProduct;
import com.magicstudios.magiccore.modules.shop.ShopService;
import com.magicstudios.magiccore.modules.shop.SellService;
import com.magicstudios.magiccore.modules.shop.PersistentSellService;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpService;
import com.magicstudios.magiccore.modules.playerwarps.PersistentPlayerWarpService;
import com.magicstudios.magiccore.modules.auction.AuctionService;
import com.magicstudios.magiccore.modules.auction.PersistentAuctionService;
import com.magicstudios.magiccore.modules.orders.OrderService;
import com.magicstudios.magiccore.modules.orders.PersistentOrderService;
import com.magicstudios.magiccore.modules.bounties.BountyService;
import com.magicstudios.magiccore.modules.bounties.PersistentBountyService;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceAnalyticsService;
import com.magicstudios.magiccore.modules.marketplace.PersistentMarketplaceAnalyticsService;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.lifesteal.PersistentLifestealService;
import com.magicstudios.magiccore.admin.MarketplaceAdminService;
import com.magicstudios.magiccore.admin.DefaultMarketplaceAdminService;
import com.magicstudios.magiccore.modules.combat.CombatService;
import com.magicstudios.magiccore.modules.combat.NativeCombatService;
import com.magicstudios.magiccore.modules.combat.AbilityCooldownService;
import com.magicstudios.magiccore.modules.combat.NewPlayerProtectionService;
import com.magicstudios.magiccore.modules.combat.TeamRelationCache;
import com.magicstudios.magiccore.modules.combat.CombatLogoutRegistry;
import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.modules.crates.PersistentCrateService;
import com.magicstudios.magiccore.modules.crates.ExternalCrateService;
import com.magicstudios.magiccore.modules.crates.CrateDefinition;
import com.magicstudios.magiccore.modules.crates.CrateCost;
import com.magicstudios.magiccore.modules.crates.CrateReward;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.PersistentPlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.CachedStatsLeaderboards;
import com.magicstudios.magiccore.modules.display.DisplayService;
import com.magicstudios.magiccore.modules.display.InternalDisplayService;
import com.magicstudios.magiccore.modules.display.TabDisplayService;
import com.magicstudios.magiccore.modules.display.CompetitiveLeaderboardService;
import com.magicstudios.magiccore.modules.display.CachedCompetitiveLeaderboards;
import com.magicstudios.magiccore.modules.store.StoreService;
import com.magicstudios.magiccore.modules.store.PersistentStoreService;
import com.magicstudios.magiccore.modules.store.ProductDefinition;
import com.magicstudios.magiccore.modules.store.ProductAction;
import com.magicstudios.magiccore.integrations.bedrock.BedrockService;
import com.magicstudios.magiccore.integrations.bedrock.FloodgateBedrockService;
import com.magicstudios.magiccore.integrations.items.CustomItemService;
import com.magicstudios.magiccore.integrations.items.ReflectiveCustomItemService;
import com.magicstudios.magiccore.integrations.discord.DiscordIntegrationService;
import com.magicstudios.magiccore.integrations.discord.DiscordSrvIntegrationService;
import com.magicstudios.magiccore.integrations.stacking.StackingCompatibilityService;
import com.magicstudios.magiccore.integrations.stacking.ReflectiveStackingCompatibilityService;
import com.magicstudios.magiccore.integrations.holograms.HologramIntegrationService;
import com.magicstudios.magiccore.integrations.holograms.DecentHologramsService;
import com.magicstudios.magiccore.integrations.npcs.NpcIntegrationService;
import com.magicstudios.magiccore.integrations.npcs.CitizensNpcService;
import com.magicstudios.magiccore.integrations.crates.ExcellentCratesProvider;
import com.magicstudios.magiccore.integrations.vulcan.VulcanService;
import com.magicstudios.magiccore.integrations.vulcan.VulcanIntegrationService;
import com.magicstudios.magiccore.integrations.vulcan.VulcanFlagBuffer;
import com.magicstudios.magiccore.integrations.apollo.LunarClientService;
import com.magicstudios.magiccore.integrations.apollo.ApolloLunarClientService;
import com.magicstudios.magiccore.imports.ImportService;
import com.magicstudios.magiccore.imports.PersistentImportService;
import com.magicstudios.magiccore.protection.AllowAllProtectionService;
import com.magicstudios.magiccore.protection.ProtectionService;
import com.magicstudios.magiccore.protection.UnavailableProtectionService;
import com.magicstudios.magiccore.protection.CompositeProtectionService;
import com.magicstudios.magiccore.placeholders.PhaseOnePlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseTwoPlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseFourPlaceholderView;
import com.magicstudios.magiccore.placeholders.PhaseFivePlaceholderView;
import com.magicstudios.magiccore.placeholders.AfkPlaceholderView;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.afk.PersistentShardService;
import com.magicstudios.magiccore.ranks.InternalRankService;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.ranks.RankService;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import com.magicstudios.magiccore.delivery.DeliveryMailbox;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.FoliaTeleportService;
import com.magicstudios.magiccore.platform.FoliaRtpService;
import com.magicstudios.magiccore.platform.BukkitInventoryRemovalPort;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.HandlerList;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class PhaseOneModuleFactory {
    private final Plugin plugin;
    private final MagicCoreConfiguration config;
    private final TransactionalDataStore store;
    private final Clock clock;
    private final AtomicConfigStore<FeaturesFile> featuresStore;
    private final SchedulerFacade scheduler;
    private final AtomicReference<AutoCloseable> vault = new AtomicReference<>();
    private final AtomicReference<AutoCloseable> papi = new AtomicReference<>();
    private final AtomicReference<PhaseOnePlayerListener> playerListener = new AtomicReference<>();
    private final AtomicReference<PhaseTwoPlayerListener> phaseTwoPlayerListener = new AtomicReference<>();
    private final AtomicReference<KillResolutionListener> killResolutionListener = new AtomicReference<>();
    private final AtomicReference<LifestealPlayerListener> lifestealPlayerListener = new AtomicReference<>();
    private final AtomicReference<CombatPlayerListener> combatPlayerListener = new AtomicReference<>();
    private final AtomicReference<FastCrystalController> fastCrystalController = new AtomicReference<>();
    private final AtomicReference<PlayerStatsListener> playerStatsListener = new AtomicReference<>();
    private final AtomicReference<InternalDisplayService> internalDisplay = new AtomicReference<>();
    private final AtomicReference<NpcClickListener> npcClickListener = new AtomicReference<>();
    private final AtomicReference<AfkShardController> afkShardController = new AtomicReference<>();
    private final AtomicReference<SpawnStashController> spawnStashController = new AtomicReference<>();
    private final AtomicReference<WorthPlayerListener> worthPlayerListener = new AtomicReference<>();
    private final AtomicReference<CustomToolController> customToolController = new AtomicReference<>();
    private final AtomicReference<KeyallController> keyallController = new AtomicReference<>();
    private final AtomicReference<com.magicstudios.magiccore.gui.MagicGuiController> guiController = new AtomicReference<>();
    private final AtomicReference<com.magicstudios.magiccore.gui.SecureStorageController> secureStorageController = new AtomicReference<>();
    private final AtomicReference<KothController> kothController = new AtomicReference<>();
    private final AtomicReference<PinataController> pinataController = new AtomicReference<>();
    private final AtomicReference<com.magicstudios.magiccore.integrations.voting.NuVotifierVoteBridge> voteBridge = new AtomicReference<>();
    private final AtomicReference<EventPresentationController> eventPresentation = new AtomicReference<>();
    private final AtomicReference<EventMaintenanceController> eventMaintenance = new AtomicReference<>();
    private final AtomicReference<com.magicstudios.magiccore.integrations.discord.DiscordBridgeHttpServer> discordBridgeServer = new AtomicReference<>();
    private final AtomicReference<org.bukkit.NamespacedKey> heartRecipeKey = new AtomicReference<>();
    private final AtomicReference<org.bukkit.NamespacedKey> revivalRecipeKey = new AtomicReference<>();

    public PhaseOneModuleFactory(Plugin plugin, MagicCoreConfiguration config,
                                 TransactionalDataStore store, Clock clock,
                                 AtomicConfigStore<FeaturesFile> featuresStore,
                                 SchedulerFacade scheduler) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.clock = clock;
        this.featuresStore = featuresStore;
        this.scheduler = scheduler;
    }

    public List<MagicModule> create() {
        List<MagicModule> modules = new ArrayList<>();
        modules.add(profiles());
        modules.add(ranks());
        modules.add(economy());
        modules.add(teams());
        modules.add(rewards());
        modules.add(crates());
        modules.add(keyalls());
        modules.add(platformIntegrations());
        modules.add(discord());
        modules.add(holograms());
        modules.add(npcs());
        modules.add(store());
        modules.add(imports());
        modules.add(settings());
        modules.add(afkShards());
        modules.add(protection());
        modules.add(spawnStash());
        modules.add(essentials());
        modules.add(playerWarps());
        modules.add(itemWorth());
        modules.add(billford());
        modules.add(customTools());
        modules.add(shop());
        modules.add(auction());
        modules.add(orders());
        modules.add(statistics());
        modules.add(resets());
        modules.add(gemshop());
        modules.add(secureStorage());
        modules.add(presentation());
        modules.add(profileViews());
        modules.add(menus());
        modules.add(koth());
        modules.add(voteParty());
        modules.add(eventOperations());
        modules.add(bounties());
        modules.add(lifesteal());
        modules.add(combat());
        modules.add(display());
        modules.add(marketplace());
        modules.add(leaderboards());
        modules.add(admin());
        modules.add(placeholders());
        modules.add(vault());
        modules.add(papi());
        return modules;
    }

    private MagicModule profiles() {
        return module("profiles", featureMode("profiles"), Set.of(), context ->
                context.services().register("profiles", PlayerProfileService.class,
                        new PersistentPlayerProfileService(store, context.events())));
    }

    private MagicModule ranks() {
        ProviderMode mode = configuredRankMode();
        return new SimpleModule(new ModuleDescriptor("ranks", mode, Set.of("profiles")), context -> {
            if ((mode == ProviderMode.LUCKPERMS || mode == ProviderMode.HYBRID)
                    && plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
                return List.of("ranks.yml system.provider is " + mode + " but LuckPerms is unavailable; install LuckPerms or set INTERNAL");
            }
            return List.of();
        }, context -> {
            RankCatalog catalog = new RankCatalog(config.ranks().system().defaultRank(), config.ranks().definitions());
            RankService ranks;
            CapabilityService capabilities;
            if (mode == ProviderMode.LUCKPERMS || mode == ProviderMode.HYBRID) {
                ranks = LuckPermsIntegrationBridge.rankService(plugin, catalog);
                capabilities = LuckPermsIntegrationBridge.capabilityService(plugin, ranks, mode == ProviderMode.HYBRID);
            } else {
                ranks = new InternalRankService(store, context.events(), catalog, clock);
                capabilities = new RankCapabilityService(ranks);
            }
            context.services().register("ranks", RankService.class, ranks);
            context.services().register("ranks", CapabilityService.class, capabilities);
        }, ignored -> { }, () -> HealthReport.healthy("ranks"));
    }

    private MagicModule economy() {
        ProviderMode mode = featureMode("economy");
        return new SimpleModule(new ModuleDescriptor("economy", mode, Set.of("profiles")), context -> {
            if (mode == ProviderMode.EXTERNAL && plugin.getServer().getPluginManager().getPlugin("Vault") == null)
                return List.of("features.yml economy is EXTERNAL but Vault is not installed; install Vault or select INTERNAL");
            if (mode == ProviderMode.EXTERNAL && !VaultIntegrationBridge.hasExternalProvider(plugin))
                return List.of("features.yml economy is EXTERNAL but Vault has no economy provider; configure one or select INTERNAL");
            if (mode != ProviderMode.INTERNAL && mode != ProviderMode.EXTERNAL)
                return List.of("economy supports INTERNAL, EXTERNAL, or DISABLED");
            return List.of();
        }, context -> {
            EconomyService economy = mode == ProviderMode.INTERNAL
                    ? new PersistentEconomyService(store, context.events(), config.economy().primaryCurrency(),
                    config.economy().definitions(), clock)
                    : VaultIntegrationBridge.consumeExternal(plugin,
                    config.economy().definitions().get(config.economy().primaryCurrency()), scheduler, store, clock);
            context.services().register("economy", EconomyService.class, economy);
        }, ignored -> { }, () -> HealthReport.healthy("economy"));
    }

    private MagicModule teams() {
        return module("teams", featureMode("teams"), Set.of("profiles", "ranks"), context -> {
            var policy = config.teams().namePolicy();
            TeamNamePolicy names = new TeamNamePolicy(policy.minimumLength(), policy.maximumLength(), policy.pattern(), policy.blockedNames());
            TeamService teams = new PersistentTeamService(store, context.services().require(CapabilityService.class),
                    context.events(), names, Duration.ofSeconds(config.teams().invitations().expiresSeconds()), clock);
            context.services().register("teams", TeamService.class, teams);
        });
    }

    private MagicModule rewards() {
        return module("rewards", featureMode("rewards"), Set.of("profiles", "economy"), context -> {
            RewardService rewards = new PersistentRewardService(store, context.events(), config.economy().definitions(),
                    config.rewards().daily().definitions(), Duration.ofHours(config.rewards().daily().cooldownHours()),
                    config.rewards().playtime().definitions(), config.rewards().playtime().policy(), clock,
                    PersistentRewardService.defaultRandom());
            context.services().register("rewards", RewardService.class, rewards);
        });
    }

    private MagicModule crates() {
        ProviderMode mode = featureMode("crates");
        Set<String>dependencies=mode==ProviderMode.INTERNAL?Set.of("profiles","economy"):Set.of("profiles");
        return new SimpleModule(new ModuleDescriptor("crates", mode, dependencies),
                ignored -> {
                    String provider=config.integrations().crates().provider().toUpperCase(java.util.Locale.ROOT);
                    if(mode==ProviderMode.INTERNAL&&featureMode("economy")!=ProviderMode.INTERNAL)return List.of("INTERNAL crates require INTERNAL economy for atomic cost/reward settlement");
                    if(mode==ProviderMode.INTERNAL&&!provider.equals("INTERNAL"))return List.of("INTERNAL crates require integrations.yml crates.provider INTERNAL");
                    if(mode==ProviderMode.EXTERNAL&&!provider.equals("EXCELLENTCRATES"))return List.of("EXTERNAL crates require integrations.yml crates.provider EXCELLENTCRATES");
                    if(mode==ProviderMode.EXTERNAL&&isFolia())return List.of("ExcellentCrates does not support Folia; use INTERNAL crates on Folia");
                    if(mode==ProviderMode.EXTERNAL&&plugin.getServer().getPluginManager().getPlugin("ExcellentCrates")==null)return List.of("ExcellentCrates provider selected but the plugin is unavailable");
                    if(mode!=ProviderMode.INTERNAL&&mode!=ProviderMode.EXTERNAL)return List.of("Crates support INTERNAL, EXTERNAL, or DISABLED mode");
                    return List.of();
                },
                context -> {
                    Map<String, CrateDefinition> definitions = config.crates().crates().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                            crate -> crate.id(), crate -> new CrateDefinition(crate.id(), crate.displayName(),
                                    new CrateCost(CrateCost.Type.valueOf(crate.cost().type()), crate.cost().keyId(), crate.cost().amount()),
                                    crate.maximumOpenAmount(), crate.rewards().stream().map(reward -> new CrateReward(reward.id(),
                                            CrateReward.Type.valueOf(reward.type()), reward.weight(), reward.rarity(), reward.material(), reward.amount(),
                                            reward.itemDataBase64(), reward.currency(), reward.amountMinor(), reward.keyId(), reward.keyAmount())).toList(),
                                    crate.milestones().stream().map(milestone -> new CrateDefinition.Milestone(milestone.openCount(), milestone.rewardId())).toList())));
                    CrateService service;
                    if(mode==ProviderMode.EXTERNAL){var provider=ExcellentCratesProvider.create(plugin);
                        for(String crateId:definitions.keySet())try{if(!provider.hasCrate(crateId))throw new IllegalStateException("ExcellentCrates is missing configured crate "+crateId);}catch(Exception failure){throw new IllegalStateException("Could not validate ExcellentCrates crate "+crateId,failure);}
                        service=new ExternalCrateService(store,provider,task->scheduler.executeGlobal(task),context.events(),clock,definitions);
                    }else service=new PersistentCrateService(store,context.events(),clock,definitions,config.economy().definitions(),config.crates().currency());
                    context.services().register("crates", CrateService.class,service);
                }, ignored -> { }, () -> HealthReport.healthy("crates"));
    }

    private MagicModule keyalls(){ProviderMode mode=config.crates().keyall().enabled()?featureMode("keyalls"):ProviderMode.DISABLED;return new SimpleModule(
            new ModuleDescriptor("keyalls",mode,Set.of("crates","profiles")),ignored->List.of(),context->{
        List<com.magicstudios.magiccore.modules.keyall.KeyallDefinition>definitions=config.crates().keyall().definitions().stream().map(value->
                new com.magicstudios.magiccore.modules.keyall.KeyallDefinition(value.id(),value.keyId(),value.amount(),
                        com.magicstudios.magiccore.modules.keyall.KeyallDefinition.Audience.valueOf(value.audience()),value.offlineDelivery(),
                        Duration.ofSeconds(value.scheduleIntervalSeconds()),value.threshold())).toList();
        var service=new com.magicstudios.magiccore.modules.keyall.PersistentKeyallService(store,context.services().require(CrateService.class),context.events(),clock,
                definitions,config.crates().keyall().maximumRecipients());context.services().register("keyalls",com.magicstudios.magiccore.modules.keyall.KeyallService.class,service);
        KeyallController controller=new KeyallController(plugin,scheduler,store,service);context.services().register("keyalls",KeyallController.class,controller);keyallController.set(controller);
        service.recover().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().severe("Keyall recovery failed: "+failure.getMessage());else{if(count>0)plugin.getLogger().info("Recovered "+count+" keyall run(s)");controller.startSchedules();}});
    },ignored->{KeyallController controller=keyallController.getAndSet(null);if(controller!=null)controller.close();},()->HealthReport.healthy("keyalls"));}

    private static boolean isFolia(){try{Class.forName("io.papermc.paper.threadedregions.RegionizedServer");return true;}catch(ClassNotFoundException ignored){return false;}}

    private MagicModule store(){ProviderMode mode=featureMode("store");return new SimpleModule(new ModuleDescriptor("store",mode,Set.of("profiles","economy","ranks","crates","essentials")),context->{
        if(mode!=ProviderMode.INTERNAL)return List.of("Store supports INTERNAL typed fulfillment or DISABLED");if(config.store().purchasesEnabled()){
            String secret=System.getenv(config.store().signatureSecretEnv());if(secret==null||secret.isBlank())return List.of("Store purchases are enabled but environment variable "+config.store().signatureSecretEnv()+" is missing");}return List.of();},context->{
        Map<String,ProductDefinition>products=config.store().products().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(product->product.id(),product->new ProductDefinition(product.id(),product.displayName(),product.minimumPaidMinor(),
                product.actions().stream().map(action->new ProductAction(ProductAction.Type.valueOf(action.type()),action.currency(),action.amountMinor(),action.keyId(),action.keyAmount(),action.material(),action.amount(),action.itemDataBase64(),action.rankId())).toList())));
        String secret=config.store().purchasesEnabled()?System.getenv(config.store().signatureSecretEnv()):"";StoreService service=new PersistentStoreService(store,context.services().require(EconomyService.class),context.services().require(CrateService.class),
                context.services().require(RankService.class),context.events(),clock,config.store().url(),config.store().purchasesEnabled(),secret,Duration.ofSeconds(config.store().signatureMaximumAgeSeconds()),
                config.store().donationGoal().enabled(),config.store().donationGoal().targetMinor(),products);context.services().register("store",StoreService.class,service);
        context.events().subscribe("store",com.magicstudios.magiccore.modules.store.PurchaseFulfilled.class,event->{scheduler.executeGlobal(()->{org.bukkit.entity.Player player=plugin.getServer().getPlayer(event.playerId());
            if(player!=null)context.services().find(PhaseTwoPlayerListener.class).ifPresent(listener->listener.deliverPending(player));if(config.store().announcePurchases())plugin.getServer().broadcast(net.kyori.adventure.text.Component.text(event.playerName()+" purchased "+event.productId()+"!"));
            context.services().find(DiscordIntegrationService.class).ifPresent(discord->discord.notify(event.playerName()+" purchased "+event.productId()+"!"));});});
    },ignored->{},()->config.store().purchasesEnabled()?HealthReport.healthy("store:webhook"):new HealthReport("store:webhook",HealthState.AVAILABLE,"Store link is active; signed purchase intake is disabled",Map.of(),clock.instant()));}

    private MagicModule platformIntegrations(){return new SimpleModule(new ModuleDescriptor("platform-integrations",ProviderMode.INTERNAL,Set.of()),ignored->List.of(),context->{
        BedrockService bedrock=FloodgateBedrockService.detect(config.integrations().bedrock().detectGeyserFloodgate(),config.integrations().bedrock().useBedrockSafeInteractions());context.services().register("platform-integrations",BedrockService.class,bedrock);
        StackingCompatibilityService stacking=ReflectiveStackingCompatibilityService.detect(plugin,config.integrations().spawners().provider());context.services().register("platform-integrations",StackingCompatibilityService.class,stacking);
        if(!config.integrations().customItems().provider().equalsIgnoreCase("NONE")){CustomItemService items=ReflectiveCustomItemService.create(config.integrations().customItems().provider(),scheduler);if(!items.available())throw new IllegalStateException("Configured custom-item provider is unavailable: "+items.provider());context.services().register("platform-integrations",CustomItemService.class,items);}
        var vulcanConfig=config.integrations().vulcan();VulcanService vulcan=VulcanIntegrationService.create(plugin,context.events(),clock,new VulcanFlagBuffer(Duration.ofSeconds(vulcanConfig.retentionSeconds()),vulcanConfig.maximumFlagsPerPlayer()),vulcanConfig.enabled(),vulcanConfig.captureFlags());context.services().register("platform-integrations",VulcanService.class,vulcan);
        var lunarConfig=config.integrations().lunarClient();LunarClientService lunar=ApolloLunarClientService.create(plugin,lunarConfig.provider(),lunarConfig.enabled());context.services().register("platform-integrations",LunarClientService.class,lunar);
    },context->context.services().find(VulcanService.class).ifPresent(VulcanService::close),()->{var vulcan=config.integrations().vulcan();boolean installed=plugin.getServer().getPluginManager().getPlugin("Vulcan")!=null;Map<String,String>details=Map.of("vulcanConfigured",Boolean.toString(vulcan.enabled()),"vulcanInstalled",Boolean.toString(installed),"behavior","FLAG_OBSERVATION_ONLY");
        return vulcan.enabled()&&!installed?new HealthReport("platform-integrations",HealthState.DEGRADED,"Vulcan integration is enabled but Vulcan is unavailable; gameplay remains active",details,clock.instant()):new HealthReport("platform-integrations",HealthState.AVAILABLE,"Platform integrations available",details,clock.instant());});}

    private MagicModule imports(){return module("imports",ProviderMode.INTERNAL,Set.of(),context->context.services().register("imports",ImportService.class,new PersistentImportService(store,clock)));}

    private MagicModule discord(){ProviderMode mode=featureMode("discord");String provider=config.integrations().discord().provider();return new SimpleModule(new ModuleDescriptor("discord",mode,Set.of()),context->{if(!config.integrations().discord().enabled())return List.of("Discord feature enabled but integrations.yml discord.enabled is false");
        if(provider.equalsIgnoreCase("DISCORDSRV")&&plugin.getServer().getPluginManager().getPlugin("DiscordSRV")==null)return List.of("DiscordSRV provider selected but plugin is unavailable");if(provider.equalsIgnoreCase("CUSTOM_BOT")){if(!config.discordBridge().enabled())return List.of("CUSTOM_BOT requires modules/discord-bridge.yml enabled: true");String secret=System.getenv(config.discordBridge().secretEnv());if(secret==null||secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32)return List.of("CUSTOM_BOT requires a 32-byte secret in "+config.discordBridge().secretEnv());}if(!Set.of("DISCORDSRV","CUSTOM_BOT").contains(provider.toUpperCase(java.util.Locale.ROOT)))return List.of("Unknown Discord provider: "+provider);return List.of();},context->{
        DiscordIntegrationService service;if(provider.equalsIgnoreCase("CUSTOM_BOT")){var configured=config.discordBridge();var bridge=new com.magicstudios.magiccore.integrations.discord.PersistentCustomDiscordBridge(store,clock,System.getenv(configured.secretEnv()).getBytes(java.nio.charset.StandardCharsets.UTF_8),Duration.ofSeconds(configured.linkCodeTtlSeconds()),Duration.ofSeconds(configured.messageMaximumAgeSeconds()),configured.maximumMessagesPerMinute(),configured.maximumRetryAttempts(),Duration.ofSeconds(configured.retryBaseSeconds()));service=bridge;context.services().register("discord",com.magicstudios.magiccore.integrations.discord.CustomDiscordBridge.class,bridge);try{var server=new com.magicstudios.magiccore.integrations.discord.DiscordBridgeHttpServer(bridge,configured.bindHost(),configured.bindPort());server.start();discordBridgeServer.set(server);}catch(java.io.IOException failure){throw new IllegalStateException("Could not bind Discord bridge transport",failure);}}else service=DiscordSrvIntegrationService.create(provider,scheduler);if(!service.available())throw new IllegalStateException("Discord provider API is unavailable");context.services().register("discord",DiscordIntegrationService.class,service);
    },ignored->{var server=discordBridgeServer.getAndSet(null);if(server!=null)server.close();},()->HealthReport.healthy("discord:"+provider));}

    private MagicModule holograms(){ProviderMode mode=featureMode("holograms");String provider=config.integrations().holograms().provider();return new SimpleModule(new ModuleDescriptor("holograms",mode,Set.of()),context->{
        if(!provider.equalsIgnoreCase("DECENT_HOLOGRAMS"))return List.of("Holograms require integrations.yml provider DECENT_HOLOGRAMS");if(plugin.getServer().getPluginManager().getPlugin("DecentHolograms")==null)return List.of("DecentHolograms is unavailable");return List.of();},context->{
        HologramIntegrationService service=DecentHologramsService.create(provider,scheduler);if(!service.available())throw new IllegalStateException("DecentHolograms API is unavailable");context.services().register("holograms",HologramIntegrationService.class,service);
    },ignored->{},()->HealthReport.healthy("holograms:"+provider));}

    private MagicModule npcs(){ProviderMode mode=featureMode("npcs");String provider=config.integrations().npcs().provider();return new SimpleModule(new ModuleDescriptor("npcs",mode,Set.of()),context->{
        if(!provider.equalsIgnoreCase("CITIZENS"))return List.of("NPCs require integrations.yml provider CITIZENS");if(plugin.getServer().getPluginManager().getPlugin("Citizens")==null)return List.of("Citizens is unavailable");return List.of();},context->{
        NpcIntegrationService service=CitizensNpcService.create(provider,scheduler);if(!service.available())throw new IllegalStateException("Citizens API is unavailable");context.services().register("npcs",NpcIntegrationService.class,service);NpcClickListener listener=new NpcClickListener(service,context.events(),clock);plugin.getServer().getPluginManager().registerEvents(listener,plugin);npcClickListener.set(listener);
    },ignored->{NpcClickListener listener=npcClickListener.getAndSet(null);if(listener!=null)HandlerList.unregisterAll(listener);},()->HealthReport.healthy("npcs:"+provider));}

    private MagicModule settings() {
        return module("settings", featureMode("settings"), Set.of("profiles"), context -> {
            Map<com.magicstudios.magiccore.modules.settings.PlayerSetting,Boolean>defaults=config.settings().defaults().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry->com.magicstudios.magiccore.modules.settings.PlayerSetting.valueOf(entry.getKey()),Map.Entry::getValue));
            context.services().register("settings", PlayerSettingsService.class,new PersistentPlayerSettingsService(store,clock,defaults));
        });
    }

    private MagicModule afkShards(){ProviderMode mode=featureMode("afk-shards");return new SimpleModule(new ModuleDescriptor("afk-shards",mode,Set.of("profiles")),context->{boolean requiresWorldGuard=config.afk().zones().stream().anyMatch(zone->zone.type().equalsIgnoreCase("WORLDGUARD"));return requiresWorldGuard&&plugin.getServer().getPluginManager().getPlugin("WorldGuard")==null?List.of("WORLDGUARD AFK zones are configured but WorldGuard is unavailable"):List.of();},context->{Set<String>zoneIds=config.afk().zones().stream().map(com.magicstudios.magiccore.config.model.AfkFile.Zone::id).collect(java.util.stream.Collectors.toUnmodifiableSet());ShardService service=new PersistentShardService(store,context.events(),clock,config.afk().policy(),config.afk().eligibility(),zoneIds);context.services().register("afk-shards",ShardService.class,service);AfkPlaceholderView view=new AfkPlaceholderView(service);view.register("afk-shards",context.placeholders(),context.events());context.services().register("afk-shards",AfkPlaceholderView.class,view);AfkShardController controller=new AfkShardController(plugin,scheduler,service,view,new AfkZoneMatcher(plugin,config.afk().zones()),clock,Duration.ofSeconds(config.afk().policy().intervalSeconds()));plugin.getServer().getPluginManager().registerEvents(controller,plugin);afkShardController.set(controller);},ignored->{AfkShardController controller=afkShardController.getAndSet(null);if(controller!=null)controller.close();},()->HealthReport.healthy("afk-shards"));}

    private MagicModule presentation(){ProviderMode mode=featureMode("presentation");return new SimpleModule(
            new ModuleDescriptor("presentation",mode,Set.of("profiles","ranks","settings","statistics","afk-shards")),
            ignored->List.of(),context->{var service=new com.magicstudios.magiccore.modules.presentation.ConfiguredPresentationService(
                    config.presentation(),context.services().require(PlayerProfileService.class),context.services().require(PlayerStatsService.class),
                    context.services().require(PlayerSettingsService.class),context.services().require(CapabilityService.class),
                    context.services().require(ShardService.class),context.services().find(DiscordIntegrationService.class),clock);
                context.services().register("presentation",com.magicstudios.magiccore.modules.presentation.PresentationService.class,service);
            },ignored->{},()->HealthReport.healthy("presentation"));}

    private MagicModule profileViews(){return new SimpleModule(new ModuleDescriptor("profile-views",ProviderMode.INTERNAL,
            Set.of("profiles","ranks","settings","statistics","afk-shards","economy")),ignored->List.of(),context->{
        var service=new com.magicstudios.magiccore.modules.profiles.DefaultProfileViewService(
                context.services().require(PlayerProfileService.class),context.services().require(PlayerSettingsService.class),
                context.services().require(PlayerStatsService.class),context.services().require(RankService.class),
                context.services().require(ShardService.class),context.services().require(CapabilityService.class),
                context.services().require(com.magicstudios.magiccore.audit.AuditService.class),context.services().require(EconomyService.class));
        context.services().register("profile-views",com.magicstudios.magiccore.modules.profiles.ProfileViewService.class,service);
    },ignored->{},()->HealthReport.healthy("profile-views"));}

    private MagicModule menus(){ProviderMode mode=featureMode("menus");Set<String>dependencies=new java.util.LinkedHashSet<>(Set.of("ranks","settings","presentation","profile-views","essentials"));for(String optional:List.of("gemshop","player-warps","shop","vaults","koth","vote-party"))if(featureMode(optional)!=ProviderMode.DISABLED)dependencies.add(optional);return new SimpleModule(
            new ModuleDescriptor("menus",mode,Set.copyOf(dependencies)),ignored->{
        List<String>issues=new ArrayList<>();java.util.function.Consumer<String>material=value->{if(org.bukkit.Material.matchMaterial(value)==null)issues.add("Unknown menu material "+value);};
        var theme=config.menus().theme();material.accept(theme.fillMaterial());material.accept(theme.accentMaterial());material.accept(theme.positiveMaterial());material.accept(theme.negativeMaterial());material.accept(theme.previousMaterial());material.accept(theme.closeMaterial());material.accept(theme.nextMaterial());
        for(var entry:config.menus().rootEntries())material.accept(entry.material());return issues;
    },context->{var items=new com.magicstudios.magiccore.gui.GuiItemFactory();var controller=new com.magicstudios.magiccore.gui.MagicGuiController(scheduler,config.menus().theme());
        controller.register(new com.magicstudios.magiccore.gui.ConfiguredRootMenu(config.menus(),context.services().require(CapabilityService.class),items,controller::hasMenu));
        var presentation=context.services().require(com.magicstudios.magiccore.modules.presentation.PresentationService.class);controller.register(new com.magicstudios.magiccore.gui.PresentationGuiMenu("info",config.menus(),presentation,items));controller.register(new com.magicstudios.magiccore.gui.PresentationGuiMenu("server",config.menus(),presentation,items));
        controller.register(new com.magicstudios.magiccore.gui.SettingsGuiMenu(config.menus(),context.services().require(PlayerSettingsService.class),items));controller.register(new com.magicstudios.magiccore.gui.ProfileGuiMenu(config.menus(),context.services().require(com.magicstudios.magiccore.modules.profiles.ProfileViewService.class),items));
        context.services().find(com.magicstudios.magiccore.modules.gemshop.GemShopService.class).ifPresent(service->controller.register(new com.magicstudios.magiccore.gui.GemShopGuiMenu(config.menus(),service,context.services().require(PhaseTwoPlayerListener.class),items)));
        context.services().find(PlayerWarpService.class).ifPresent(service->controller.register(new com.magicstudios.magiccore.gui.PlayerWarpsGuiMenu(config.menus(),service,items)));if(context.services().find(com.magicstudios.magiccore.modules.shop.AdvancedSellService.class).isPresent())controller.register(new com.magicstudios.magiccore.gui.SellingGuiMenu(config.menus(),items));
        context.services().find(com.magicstudios.magiccore.modules.securestorage.SecureStorageService.class).ifPresent(service->{var storageController=new com.magicstudios.magiccore.gui.SecureStorageController(plugin,scheduler,service,config.secureStorage());plugin.getServer().getPluginManager().registerEvents(storageController,plugin);context.services().register("menus",com.magicstudios.magiccore.gui.SecureStorageController.class,storageController);secureStorageController.set(storageController);controller.register(new com.magicstudios.magiccore.gui.StorageAccessGuiMenu(config.menus(),config.secureStorage(),storageController,items));});
        var kothService=context.services().find(com.magicstudios.magiccore.modules.events.KothService.class);var voteService=context.services().find(com.magicstudios.magiccore.modules.events.VotePartyService.class);if(kothService.isPresent()&&voteService.isPresent())controller.register(new com.magicstudios.magiccore.gui.EventStatusGuiMenu(config.menus(),kothService.get(),voteService.get(),items));
        plugin.getServer().getPluginManager().registerEvents(controller,plugin);context.services().register("menus",com.magicstudios.magiccore.gui.MagicGuiController.class,controller);guiController.set(controller);
    },ignored->{var storage=secureStorageController.getAndSet(null);if(storage!=null)storage.close();var controller=guiController.getAndSet(null);if(controller!=null)controller.close();},()->HealthReport.healthy("menus"));}

    private MagicModule koth(){ProviderMode mode=featureMode("koth");return new SimpleModule(new ModuleDescriptor("koth",mode,Set.of("teams","economy")),ignored->{List<String>issues=new ArrayList<>();for(var value:config.events().koth().definitions())for(String material:value.bannedMaterials())if(org.bukkit.Material.matchMaterial(material)==null)issues.add("Unknown KOTH banned material "+material);return issues;},context->{List<com.magicstudios.magiccore.modules.events.KothDefinition>definitions=config.events().koth().definitions().stream().filter(com.magicstudios.magiccore.config.model.EventsFile.KothDefinition::enabled).map(value->new com.magicstudios.magiccore.modules.events.KothDefinition(value.id(),value.displayName(),value.world(),value.minimumX(),value.minimumY(),value.minimumZ(),value.maximumX(),value.maximumY(),value.maximumZ(),Duration.ofSeconds(value.captureSeconds()),Duration.ofSeconds(value.firstDelaySeconds()),Duration.ofSeconds(value.scheduleIntervalSeconds()),Set.copyOf(value.bannedMaterials()),value.reward())).toList();var service=new com.magicstudios.magiccore.modules.events.PersistentKothService(store,clock,context.events(),definitions,config.economy().definitions());context.services().register("koth",com.magicstudios.magiccore.modules.events.KothService.class,service);var controller=new KothController(plugin,scheduler,service,context.services().require(TeamService.class),clock,Duration.ofSeconds(config.events().koth().tickSeconds()));plugin.getServer().getPluginManager().registerEvents(controller,plugin);controller.start();context.services().register("koth",KothController.class,controller);kothController.set(controller);},ignored->{var controller=kothController.getAndSet(null);if(controller!=null)controller.close();},()->HealthReport.healthy("koth"));}

    private MagicModule voteParty(){ProviderMode mode=featureMode("vote-party");return new SimpleModule(new ModuleDescriptor("vote-party",mode,Set.of("profiles","economy","settings")),ignored->{List<String>issues=new ArrayList<>();try{org.bukkit.entity.EntityType.valueOf(config.events().voteParty().pinata().entityType());}catch(IllegalArgumentException failure){issues.add("Unknown pinata entity type "+config.events().voteParty().pinata().entityType());}return issues;},context->{var service=new com.magicstudios.magiccore.modules.events.PersistentVotePartyService(store,clock,context.events(),config.events().voteParty(),config.economy().definitions());context.services().register("vote-party",com.magicstudios.magiccore.modules.events.VotePartyService.class,service);var controller=new PinataController(plugin,scheduler,service,context.services().require(PlayerSettingsService.class),config.events().voteParty(),clock);plugin.getServer().getPluginManager().registerEvents(controller,plugin);controller.start();context.services().register("vote-party",PinataController.class,controller);pinataController.set(controller);var bridge=com.magicstudios.magiccore.integrations.voting.NuVotifierVoteBridge.register(plugin,context.services().require(PlayerProfileService.class),controller,clock);voteBridge.set(bridge);},ignored->{close(voteBridge.getAndSet(null));var controller=pinataController.getAndSet(null);if(controller!=null)controller.close();},()->plugin.getServer().getPluginManager().getPlugin("Votifier")==null?new HealthReport("vote-party",HealthState.AVAILABLE,"Vote-party is ready; NuVotifier is not installed, so only capability-gated administration can submit votes",Map.of("trusted-provider","UNAVAILABLE"),clock.instant()):HealthReport.healthy("vote-party:nuvotifier"));}

    private MagicModule eventOperations(){return new SimpleModule(new ModuleDescriptor("event-operations",ProviderMode.INTERNAL,Set.of("settings","player-warps","vaults")),ignored->{List<String>issues=new ArrayList<>();for(var value:config.events().announcements())if(value.sound()!=null&&!value.sound().isBlank()&&org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(value.sound().toLowerCase(java.util.Locale.ROOT)))==null)issues.add("Unknown announcement sound "+value.sound());return issues;},context->{var presentation=new EventPresentationController(plugin,scheduler,context.services().require(PlayerSettingsService.class),context.events(),config.events().announcements());presentation.start();eventPresentation.set(presentation);var maintenanceConfig=config.events().maintenance();var maintenance=new EventMaintenanceController(plugin,scheduler,context.services().require(PlayerWarpService.class),context.services().require(com.magicstudios.magiccore.modules.securestorage.SecureStorageService.class),Duration.ofSeconds(maintenanceConfig.sponsorshipExpiryIntervalSeconds()),Duration.ofSeconds(maintenanceConfig.secureStorageRecoveryIntervalSeconds()));maintenance.start();eventMaintenance.set(maintenance);},ignored->{close(eventMaintenance.getAndSet(null));close(eventPresentation.getAndSet(null));},()->HealthReport.healthy("event-operations"));}

    private MagicModule protection() {
        return new SimpleModule(new ModuleDescriptor("protection", ProviderMode.INTERNAL, Set.of()), ignored -> List.of(), context -> {
            boolean configured = config.integrations().worldguard().enabled();
            boolean installed = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
            ProtectionService regions = !configured ? new AllowAllProtectionService()
                    : installed ? WorldGuardIntegrationBridge.create(plugin)
                    : new UnavailableProtectionService("WORLDGUARD");
            ProtectionService claims = ClaimsIntegrationBridge.create(plugin, config.integrations().claims().provider());
            ProtectionService service = new CompositeProtectionService(List.of(regions, claims));
            context.services().register("protection", ProtectionService.class, service);
        }, ignored -> { }, () -> {
            if (config.integrations().worldguard().enabled()
                    && plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                return new HealthReport("protection", HealthState.DEGRADED,
                        "WorldGuard is configured but unavailable; dependent actions fail closed", Map.of(), clock.instant());
            }
            return HealthReport.healthy("protection");
        });
    }

    private MagicModule spawnStash(){ProviderMode mode=featureMode("spawn-stash");return new SimpleModule(
            new ModuleDescriptor("spawn-stash",mode,Set.of("profiles","ranks","protection","platform-integrations")),context->{
                List<String> issues=new ArrayList<>();for(var definition:config.spawnStash().decoyBlocks()){try{org.bukkit.Bukkit.createBlockData(definition.blockData());}catch(IllegalArgumentException failure){issues.add("Invalid SpawnStash block-data: "+definition.blockData());}
                    for(var loot:definition.lootAppearance())if(org.bukkit.Material.matchMaterial(loot.material())==null)issues.add("Invalid SpawnStash loot material: "+loot.material());}return issues;
            },context->{var service=new com.magicstudios.magiccore.modules.spawnstash.PersistentSpawnStashService(store,context.events(),context.services().require(com.magicstudios.magiccore.audit.AuditService.class),clock,config.spawnStash().observeOnly());
                context.services().register("spawn-stash",com.magicstudios.magiccore.modules.spawnstash.SpawnStashService.class,service);var controller=new SpawnStashController(plugin,scheduler,service,
                        context.services().require(ProtectionService.class),context.services().require(CapabilityService.class),context.services().require(VulcanService.class),context.events(),config.spawnStash(),clock);
                plugin.getServer().getPluginManager().registerEvents(controller,plugin);context.services().register("spawn-stash",SpawnStashController.class,controller);spawnStashController.set(controller);
            },ignored->{SpawnStashController controller=spawnStashController.getAndSet(null);if(controller!=null)controller.close();},()->{
                boolean vulcan=contextVulcanAvailable();return vulcan?HealthReport.healthy("spawn-stash:vulcan-evidence"):new HealthReport("spawn-stash",HealthState.DEGRADED,"SpawnStash is active; Vulcan flag evidence is unavailable",Map.of("behavior","OBSERVE_ONLY"),clock.instant());});}

    private boolean contextVulcanAvailable(){return plugin.getServer().getPluginManager().getPlugin("Vulcan")!=null&&config.integrations().vulcan().enabled();}

    private MagicModule essentials() {
        ProviderMode mode = featureMode("essentials");
        Set<String> dependencies = new java.util.LinkedHashSet<>(Set.of("profiles", "ranks", "settings", "protection"));
        if (config.essentials().teleport().costMinor() > 0) dependencies.add("economy");
        return new SimpleModule(new ModuleDescriptor("essentials", mode, Set.copyOf(dependencies)),
                ignored -> config.essentials().teleport().costMinor() > 0 && featureMode("economy") != ProviderMode.INTERNAL
                        ? List.of("Paid teleports require INTERNAL economy so debit/refund can be atomic") : List.of(), context -> {
            var capabilities = context.services().require(CapabilityService.class);
            var settings = context.services().require(PlayerSettingsService.class);
            com.magicstudios.magiccore.admin.CapabilityGate adminGate = (actor, capability) -> actor.console()
                    ? java.util.concurrent.CompletableFuture.completedFuture(true)
                    : capabilities.has(actor.playerId(), capability);
            context.services().register("essentials", HomeService.class, new PersistentHomeService(store, capabilities,
                    context.events(), clock, config.essentials().homes().maximumNameLength()));
            context.services().register("essentials", WarpService.class,
                    new PersistentWarpService(store, context.services().require(RankService.class), capabilities,
                            adminGate, clock, config.essentials().homes().maximumNameLength()));
            BackService back = new PersistentBackService(store, clock);
            context.services().register("essentials", BackService.class, back);
            context.services().register("essentials", TeleportRequestService.class, new TeleportRequestService(settings,
                    clock, Duration.ofSeconds(config.essentials().teleport().requestLifetimeSeconds())));
            TeleportWarmupService warmups = new TeleportWarmupService(clock,
                    config.essentials().teleport().movementTolerance());
            context.services().register("essentials", TeleportWarmupService.class, warmups);
            var teleportConfig = config.essentials().teleport();
            TeleportPolicyService teleportPolicy = new PersistentTeleportPolicyService(store,
                    config.economy().definitions().get(teleportConfig.currency()), teleportConfig.costMinor(),
                    Duration.ofSeconds(teleportConfig.cooldownSeconds()), clock);
            context.services().register("essentials", TeleportPolicyService.class, teleportPolicy);
            context.services().register("essentials", TeleportService.class, new FoliaTeleportService(scheduler,
                    warmups, back, context.services().require(ProtectionService.class), teleportPolicy,
                    () -> context.services().find(CombatService.class)));
            var rtpConfig = config.essentials().rtp();
            context.services().register("essentials", RtpBounds.class, new RtpBounds(rtpConfig.centerX(), rtpConfig.centerZ(),
                    rtpConfig.minimumRadius(), rtpConfig.maximumRadius(), rtpConfig.maximumAttempts()));
            context.services().register("essentials", RtpService.class, new FoliaRtpService(scheduler,
                    context.services().require(TeleportService.class), context.services().require(ProtectionService.class),
                    Duration.ofSeconds(teleportConfig.warmupSeconds())));
            List<KitDefinition> kits = config.essentials().kits().stream().map(kit -> new KitDefinition(kit.id(),
                    kit.displayName(), Duration.ofSeconds(kit.cooldownSeconds()), kit.capability(), kit.items().stream()
                    .map(item -> new KitDefinition.KitItem(item.material(), item.amount(), item.itemDataBase64())).toList())).toList();
            context.services().register("essentials", KitService.class,
                    new PersistentKitService(store, capabilities, clock, kits));
            PhaseTwoPlayerListener listener = new PhaseTwoPlayerListener(plugin, scheduler,
                    context.services().require(TeleportService.class), back,
                    context.services().require(DeliveryMailbox.class));
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            context.services().register("essentials", PhaseTwoPlayerListener.class, listener);
            phaseTwoPlayerListener.set(listener);
        }, ignored -> {
            PhaseTwoPlayerListener listener = phaseTwoPlayerListener.getAndSet(null);
            if (listener != null) HandlerList.unregisterAll(listener);
        }, () -> HealthReport.healthy("essentials"));
    }

    private MagicModule shop() {
        return module("shop", featureMode("shop"), Set.of("profiles", "economy", "item-worth"), context -> {
            List<ShopProduct> products = config.shop().products().stream().map(product -> new ShopProduct(product.id(),
                    product.category(), product.material(), product.amount(), product.buyPriceMinor(),
                    product.sellPriceMinor(), product.itemDataBase64())).toList();
            context.services().register("shop", ShopService.class, new InternalShopService(store,
                    config.economy().definitions().get(config.shop().currency()), clock, products));
            var inventoryPort=new BukkitInventoryRemovalPort(plugin,scheduler);context.services().register("shop", SellService.class, new PersistentSellService(store,
                    inventoryPort,
                    config.economy().definitions().get(config.shop().currency()), clock, Duration.ofSeconds(30), products));
            var advanced=new com.magicstudios.magiccore.modules.shop.PersistentAdvancedSellService(store,inventoryPort,context.services().require(com.magicstudios.magiccore.modules.worth.ItemValuationService.class),
                    config.economy().definitions().get(config.itemWorth().currency()),context.services().require(com.magicstudios.magiccore.audit.AuditService.class),clock,Duration.ofSeconds(30));context.services().register("shop",com.magicstudios.magiccore.modules.shop.AdvancedSellService.class,advanced);
            advanced.recoverRemoved().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().severe("Advanced sell recovery failed: "+failure.getMessage());else if(count>0)plugin.getLogger().info("Recovered "+count+" removed sell quote(s)");});
        });
    }

    private MagicModule itemWorth(){ProviderMode mode=featureMode("item-worth");return new SimpleModule(new ModuleDescriptor("item-worth",mode,Set.of("economy")),ignored->List.of(),context->{
        var valuation=new com.magicstudios.magiccore.modules.worth.ConfiguredItemValuationService(config.itemWorth());context.services().register("item-worth",com.magicstudios.magiccore.modules.worth.ItemValuationService.class,valuation);
        var view=new com.magicstudios.magiccore.placeholders.WorthPlaceholderView(valuation);view.register("item-worth",context.placeholders());context.services().register("item-worth",com.magicstudios.magiccore.placeholders.WorthPlaceholderView.class,view);
        var listener=new WorthPlayerListener(scheduler,valuation,view);plugin.getServer().getPluginManager().registerEvents(listener,plugin);worthPlayerListener.set(listener);plugin.getServer().getOnlinePlayers().forEach(listener::refresh);
    },ignored->{WorthPlayerListener listener=worthPlayerListener.getAndSet(null);if(listener!=null)HandlerList.unregisterAll(listener);},()->HealthReport.healthy("item-worth"));}

    private MagicModule billford(){ProviderMode mode=featureMode("billford-trade");return new SimpleModule(new ModuleDescriptor("billford-trade",mode,Set.of("profiles","economy","item-worth")),ignored->List.of(),context->{List<com.magicstudios.magiccore.modules.billford.BillfordRecipe>recipes=config.billford().recipes().stream().map(recipe->new com.magicstudios.magiccore.modules.billford.BillfordRecipe(recipe.id(),recipe.displayName(),recipe.stock(),recipe.perPlayerLimit(),Duration.ofSeconds(recipe.cooldownSeconds()),recipe.ingredients().stream().map(value->new com.magicstudios.magiccore.modules.billford.BillfordIngredient(value.itemId(),value.amount())).toList(),recipe.rewards().stream().map(value->new com.magicstudios.magiccore.modules.billford.BillfordReward(value.id(),com.magicstudios.magiccore.modules.billford.BillfordReward.Type.valueOf(value.type()),value.weight(),value.currency(),value.amountMinor(),value.material(),value.amount(),value.itemDataBase64())).toList())).toList();
        var service=new com.magicstudios.magiccore.modules.billford.PersistentBillfordService(store,new BukkitInventoryRemovalPort(plugin,scheduler),recipes,config.economy().definitions(),context.services().require(com.magicstudios.magiccore.audit.AuditService.class),clock);context.services().register("billford-trade",com.magicstudios.magiccore.modules.billford.BillfordService.class,service);service.recover().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().severe("Billford recovery failed: "+failure.getMessage());else if(count>0)plugin.getLogger().info("Recovered "+count+" Billford exchange(s)");});
    },ignored->{},()->HealthReport.healthy("billford-trade"));}

    private MagicModule customTools(){ProviderMode mode=featureMode("custom-tools");return new SimpleModule(new ModuleDescriptor("custom-tools",mode,Set.of("profiles","protection")),ignored->{List<String>issues=new ArrayList<>();for(var tool:config.tools().tools()){if(org.bukkit.Material.matchMaterial(tool.material())==null)issues.add("Unknown tool material "+tool.material());for(String block:tool.blockAllowlist())if(org.bukkit.Material.matchMaterial(block)==null)issues.add("Unknown tool block "+block);for(String ingredient:tool.recipe().ingredients().values())if(org.bukkit.Material.matchMaterial(ingredient)==null)issues.add("Unknown recipe material "+ingredient);}return issues;},context->{List<com.magicstudios.magiccore.modules.tools.ToolDefinition>definitions=config.tools().tools().stream().map(tool->new com.magicstudios.magiccore.modules.tools.ToolDefinition(tool.id(),tool.material(),tool.displayName(),tool.durability(),Duration.ofMillis(tool.cooldownMillis()),tool.dropPolicy(),Set.copyOf(tool.blockAllowlist()),tool.upgrades().stream().map(value->new com.magicstudios.magiccore.modules.tools.ToolUpgrade(value.level(),com.magicstudios.magiccore.modules.tools.ToolUpgrade.Shape.valueOf(value.shape()),value.radius(),value.depth(),value.dropMultiplier())).toList())).toList();var service=new com.magicstudios.magiccore.modules.tools.InMemoryCustomToolService(definitions);context.services().register("custom-tools",com.magicstudios.magiccore.modules.tools.CustomToolService.class,service);var controller=new CustomToolController(plugin,scheduler,service,context.services().require(ProtectionService.class),clock,config.tools().tools());plugin.getServer().getPluginManager().registerEvents(controller,plugin);context.services().register("custom-tools",CustomToolController.class,controller);customToolController.set(controller);},ignored->{CustomToolController controller=customToolController.getAndSet(null);if(controller!=null)controller.close();},()->HealthReport.healthy("custom-tools"));}

    private MagicModule playerWarps() {
        return module("player-warps", featureMode("player-warps"), Set.of("profiles", "ranks", "protection", "economy"), context ->
                context.services().register("player-warps", PlayerWarpService.class,
                        new PersistentPlayerWarpService(store, context.services().require(CapabilityService.class),
                                context.services().require(ProtectionService.class), clock,
                                config.essentials().homes().maximumNameLength(),Set.copyOf(config.playerWarps().categories()),
                                Duration.ofSeconds(config.playerWarps().defaultExpirySeconds()),config.playerWarps().sponsorship().enabled(),
                                config.economy().definitions().get(config.playerWarps().sponsorship().currency()),config.playerWarps().sponsorship().pricePerHourMinor(),
                                Duration.ofSeconds(config.playerWarps().sponsorship().minimumDurationSeconds()),Duration.ofSeconds(config.playerWarps().sponsorship().maximumDurationSeconds()),
                                config.playerWarps().sponsorship().maximumActiveGlobal(),config.playerWarps().sponsorship().maximumActivePerPlayer())));
    }

    private MagicModule auction() {
        ProviderMode mode = featureMode("auctions");
        return new SimpleModule(new ModuleDescriptor("auctions", mode, Set.of("profiles", "ranks", "economy")),
                ignored -> featureMode("economy") != ProviderMode.INTERNAL
                        ? List.of("Internal auctions require INTERNAL economy for atomic buyer/seller settlement") : List.of(), context -> {
            var configured = config.auction();
            context.services().register("auctions", AuctionService.class, new PersistentAuctionService(store,
                    context.services().require(CapabilityService.class), new BukkitInventoryRemovalPort(plugin, scheduler),
                    config.economy().definitions().get(configured.currency()), context.events(), clock,
                    Duration.ofSeconds(configured.minimumDurationSeconds()),
                    Duration.ofSeconds(configured.maximumDurationSeconds()), configured.minimumPriceMinor(),
                    configured.maximumPriceMinor(), configured.listingFeeMinor(), Set.copyOf(configured.categories())));
        }, ignored -> { }, () -> HealthReport.healthy("auctions"));
    }

    private MagicModule orders() {
        ProviderMode mode=featureMode("orders");
        return new SimpleModule(new ModuleDescriptor("orders",mode,Set.of("profiles","ranks","economy")),
                ignored->featureMode("economy")!=ProviderMode.INTERNAL?List.of("Internal orders require INTERNAL economy for escrow settlement"):List.of(),context->{
            var configured=config.orders();context.services().register("orders",OrderService.class,new PersistentOrderService(store,
                    context.services().require(CapabilityService.class),new BukkitInventoryRemovalPort(plugin,scheduler),
                    config.economy().definitions().get(configured.currency()),clock,
                    Duration.ofSeconds(configured.minimumDurationSeconds()),Duration.ofSeconds(configured.maximumDurationSeconds()),
                    configured.minimumUnitPriceMinor(),configured.maximumUnitPriceMinor(),Set.copyOf(configured.categories())));
        },ignored->{},()->HealthReport.healthy("orders"));
    }

    private MagicModule bounties(){ProviderMode mode=featureMode("bounties");return new SimpleModule(
            new ModuleDescriptor("bounties",mode,Set.of("profiles","economy","statistics")),
            ignored->featureMode("economy")!=ProviderMode.INTERNAL?List.of("Internal bounties require INTERNAL economy for atomic escrow claims"):List.of(),context->{
        var configured=config.bounties();BountyService service=new PersistentBountyService(store,
                config.economy().definitions().get(configured.currency()),context.events(),clock,configured.minimumAmountMinor(),
                configured.maximumAmountMinor(),configured.taxBasisPoints(),configured.maximumContributionsPerTarget(),context.services().require(PlayerStatsService.class),
                context.services().require(PlayerProfileService.class),configured.restrictions().enabled(),configured.restrictions().minimumTargetPlaytimeSeconds(),configured.restrictions().minimumTargetKills());
        context.services().register("bounties",BountyService.class,service);
    },ignored->{},
            ()->HealthReport.healthy("bounties"));}

    private MagicModule statistics(){return new SimpleModule(new ModuleDescriptor("statistics",ProviderMode.INTERNAL,Set.of("profiles")),ignored->List.of(),context->{
        PlayerStatsService stats=new PersistentPlayerStatsService(store,context.events(),clock);context.services().register("statistics",PlayerStatsService.class,stats);
        CachedStatsLeaderboards leaderboards=new CachedStatsLeaderboards(stats,clock,Duration.ofSeconds(config.display().leaderboardCacheSeconds()));context.services().register("statistics",CachedStatsLeaderboards.class,leaderboards);
        context.events().subscribe("statistics",com.magicstudios.magiccore.modules.statistics.StatsChanged.class,event->leaderboards.invalidate());
        PlayerStatsListener listener=new PlayerStatsListener(stats,clock);plugin.getServer().getPluginManager().registerEvents(listener,plugin);playerStatsListener.set(listener);
    },ignored->{PlayerStatsListener listener=playerStatsListener.getAndSet(null);if(listener!=null){HandlerList.unregisterAll(listener);listener.close();}},()->HealthReport.healthy("statistics"));}

    private MagicModule resets(){return module("resets",ProviderMode.INTERNAL,Set.of("statistics"),context->
            context.services().register("resets",com.magicstudios.magiccore.modules.resets.ResetAdminService.class,
                    new com.magicstudios.magiccore.modules.resets.PersistentResetAdminService(store,
                            context.services().require(com.magicstudios.magiccore.audit.AuditService.class),context.events(),clock,Duration.ofMinutes(5))));}

    private MagicModule gemshop(){ProviderMode mode=config.shop().gemShop().enabled()?featureMode("gemshop"):ProviderMode.DISABLED;return new SimpleModule(
            new ModuleDescriptor("gemshop",mode,Set.of("economy","ranks","statistics","shop")),ignored->{List<String>issues=new ArrayList<>();
        for(var product:config.shop().gemShop().products())if(org.bukkit.Material.matchMaterial(product.material())==null)issues.add("Unknown GemShop material: "+product.material());return issues;},context->{
        var configured=config.shop().gemShop();List<com.magicstudios.magiccore.modules.gemshop.GemProduct>products=configured.products().stream().map(value->
                new com.magicstudios.magiccore.modules.gemshop.GemProduct(value.id(),value.category(),value.displayName(),value.material(),value.amount(),value.itemDataBase64(),value.priceMinor(),value.requiredCapability(),value.minimumPlaytimeSeconds(),value.minimumKills())).toList();
        var service=new com.magicstudios.magiccore.modules.gemshop.PersistentGemShopService(store,config.economy().definitions().get(configured.currency()),
                context.services().require(CapabilityService.class),context.services().require(PlayerStatsService.class),clock,Duration.ofSeconds(configured.confirmationSeconds()),products);
        context.services().register("gemshop",com.magicstudios.magiccore.modules.gemshop.GemShopService.class,service);
        var view=new com.magicstudios.magiccore.placeholders.GemBalancePlaceholderView(context.services().require(EconomyService.class),configured.currency());view.register("gemshop",context.placeholders(),context.events());context.services().register("gemshop",com.magicstudios.magiccore.placeholders.GemBalancePlaceholderView.class,view);plugin.getServer().getOnlinePlayers().forEach(player->view.refresh(player.getUniqueId()));
    },ignored->{},()->HealthReport.healthy("gemshop"));}

    private MagicModule secureStorage(){ProviderMode mode=featureMode("vaults");return module("vaults",mode,Set.of("ranks"),context->{var configured=config.secureStorage();
        var service=new com.magicstudios.magiccore.modules.securestorage.PersistentSecureStorageService(store,context.services().require(CapabilityService.class),clock,
                Duration.ofSeconds(configured.leaseSeconds()),configured.maximumVaults(),configured.vaultRowsLimit(),configured.maximumItemPayloadBytes(),configured.maximumContainerPayloadBytes(),
                com.magicstudios.magiccore.modules.securestorage.PersistentSecureStorageService.NestedContainerPolicy.valueOf(configured.nestedContainerPolicy()),
                com.magicstudios.magiccore.modules.securestorage.PersistentSecureStorageService.CustomItemPolicy.valueOf(configured.customItemPolicy()),configured.adminCapability());
        context.services().register("vaults",com.magicstudios.magiccore.modules.securestorage.SecureStorageService.class,service);service.recoverExpired().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().severe("Secure storage lease recovery failed: "+failure.getMessage());else if(count>0)plugin.getLogger().info("Recovered "+count+" expired secure storage lease(s)");});});}

    private MagicModule lifesteal(){ProviderMode mode=featureMode("lifesteal");return new SimpleModule(new ModuleDescriptor("lifesteal",mode,Set.of("profiles","bounties","statistics")),
            ignored->{List<String>errors=new ArrayList<>();var configured=config.lifesteal();
                try{org.bukkit.Material.valueOf(configured.heartItem().material());}catch(IllegalArgumentException failure){errors.add("Unknown heart item material: "+configured.heartItem().material());}
                try{org.bukkit.Material.valueOf(configured.revivalItem().material());}catch(IllegalArgumentException failure){errors.add("Unknown revival item material: "+configured.revivalItem().material());}
                java.util.stream.Stream.of(configured.recipe(),configured.revivalRecipe()).forEach(recipe->recipe.ingredients().forEach((symbol,material)->{if(symbol.length()!=1)errors.add("Lifesteal recipe symbols must be one character: "+symbol);try{org.bukkit.Material.valueOf(material);}catch(IllegalArgumentException failure){errors.add("Unknown Lifesteal recipe material: "+material);}}));
                return errors;},context->{var configured=config.lifesteal();LifestealService service=new PersistentLifestealService(store,context.events(),clock,
        configured.startingHearts(),configured.minimumHearts(),configured.maximumHearts(),configured.revivalHearts(),Duration.ofSeconds(configured.samePlayerCooldownSeconds()),
        PersistentLifestealService.NonPlayerDeathPolicy.valueOf(configured.nonPlayerDeathPolicy()),configured.heartItem().material(),configured.heartItem().displayName());
        context.services().register("lifesteal",LifestealService.class,service);CombatLogoutRegistry logoutRegistry=new CombatLogoutRegistry();context.services().register("lifesteal",CombatLogoutRegistry.class,logoutRegistry);
        KillResolutionListener listener=new KillResolutionListener(context.services().require(BountyService.class),service,context.services().require(PlayerStatsService.class),clock,logoutRegistry);
        plugin.getServer().getPluginManager().registerEvents(listener,plugin);killResolutionListener.set(listener);
        LifestealPlayerListener playerListener=new LifestealPlayerListener(plugin,scheduler,service,context.events(),configured.eliminationAction(),configured.revivalEnabled());
        plugin.getServer().getPluginManager().registerEvents(playerListener,plugin);lifestealPlayerListener.set(playerListener);
        if(configured.recipe().enabled()){
            org.bukkit.NamespacedKey key=new org.bukkit.NamespacedKey(plugin,"heart");org.bukkit.inventory.ItemStack result=new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(configured.heartItem().material()));
            result.editMeta(meta->{meta.displayName(new com.magicstudios.magiccore.text.MiniMessageRenderer().render(configured.heartItem().displayName()));meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin,"heart_item"),org.bukkit.persistence.PersistentDataType.STRING,"HEART");});
            org.bukkit.inventory.ShapedRecipe recipe=new org.bukkit.inventory.ShapedRecipe(key,result);recipe.shape(configured.recipe().shape().toArray(String[]::new));
            configured.recipe().ingredients().forEach((symbol,material)->recipe.setIngredient(symbol.charAt(0),org.bukkit.Material.valueOf(material)));
            plugin.getServer().addRecipe(recipe);heartRecipeKey.set(key);
        }
        if(configured.revivalEnabled()&&configured.revivalRecipe().enabled()){
            org.bukkit.NamespacedKey key=new org.bukkit.NamespacedKey(plugin,"revival_heart");org.bukkit.inventory.ItemStack result=new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(configured.revivalItem().material()));
            result.editMeta(meta->{meta.displayName(new com.magicstudios.magiccore.text.MiniMessageRenderer().render(configured.revivalItem().displayName()));meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin,"heart_item"),org.bukkit.persistence.PersistentDataType.STRING,"REVIVAL");});
            org.bukkit.inventory.ShapedRecipe recipe=new org.bukkit.inventory.ShapedRecipe(key,result);recipe.shape(configured.revivalRecipe().shape().toArray(String[]::new));
            configured.revivalRecipe().ingredients().forEach((symbol,material)->recipe.setIngredient(symbol.charAt(0),org.bukkit.Material.valueOf(material)));
            plugin.getServer().addRecipe(recipe);revivalRecipeKey.set(key);
        }
    },ignored->{KillResolutionListener listener=killResolutionListener.getAndSet(null);if(listener!=null)HandlerList.unregisterAll(listener);
        LifestealPlayerListener playerListener=lifestealPlayerListener.getAndSet(null);if(playerListener!=null){HandlerList.unregisterAll(playerListener);playerListener.close();}
        org.bukkit.NamespacedKey key=heartRecipeKey.getAndSet(null);if(key!=null)plugin.getServer().removeRecipe(key);
        org.bukkit.NamespacedKey revivalKey=revivalRecipeKey.getAndSet(null);if(revivalKey!=null)plugin.getServer().removeRecipe(revivalKey);},()->HealthReport.healthy("lifesteal"));}

    private MagicModule combat(){ProviderMode mode=featureMode("combat");return new SimpleModule(new ModuleDescriptor("combat",mode,
            Set.of("profiles","teams","protection","lifesteal","bounties","statistics","settings","platform-integrations")),context->{List<String>errors=new ArrayList<>();
        for(String material:config.combat().restrictedItems())try{org.bukkit.Material.valueOf(material);}catch(IllegalArgumentException failure){errors.add("Unknown restricted combat material: "+material);}
        for(String material:config.combat().fastCrystal().baseBlocks())try{org.bukkit.Material.valueOf(material);}catch(IllegalArgumentException failure){errors.add("Unknown Fast Crystal base block: "+material);}
        if(org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(config.combat().fastCrystal().sound().toLowerCase(java.util.Locale.ROOT)))==null)errors.add("Unknown Fast Crystal sound: "+config.combat().fastCrystal().sound());return errors;},context->{
        var configured=config.combat();CombatService combat=new NativeCombatService(clock,Duration.ofSeconds(configured.tagDurationSeconds()));
        NewPlayerProtectionService newbies=new NewPlayerProtectionService(context.services().require(PlayerProfileService.class),clock,
                Duration.ofSeconds(configured.newbieProtection().enabled()?configured.newbieProtection().durationSeconds():0));
        TeamRelationCache teams=new TeamRelationCache(context.services().require(TeamService.class));AbilityCooldownService cooldowns=new AbilityCooldownService(clock);
        context.events().subscribe("combat",com.magicstudios.magiccore.modules.teams.TeamChanged.class,event->teams.invalidateAllAndRefresh());
        context.services().register("combat",CombatService.class,combat);context.services().register("combat",NewPlayerProtectionService.class,newbies);
        context.services().register("combat",TeamRelationCache.class,teams);context.services().register("combat",AbilityCooldownService.class,cooldowns);
        Set<org.bukkit.Material>restricted=configured.restrictedItems().stream().map(org.bukkit.Material::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        CombatPlayerListener listener=new CombatPlayerListener(combat,newbies,teams,context.services().require(ProtectionService.class),cooldowns,
                context.services().require(CombatLogoutRegistry.class),context.services().require(LifestealService.class),context.services().require(BountyService.class),
                context.services().require(PlayerStatsService.class),
                Set.copyOf(configured.restrictedCommands()),restricted,Duration.ofMillis(configured.enderPearlCooldownMillis()),Duration.ofMillis(configured.tridentCooldownMillis()),
                configured.newbieProtection().removeOnAttack(),config.teams().friendlyFire());
        plugin.getServer().getPluginManager().registerEvents(listener,plugin);combatPlayerListener.set(listener);
        FastCrystalController crystalController=new FastCrystalController(scheduler,context.services().require(PlayerSettingsService.class),
                context.services().require(ProtectionService.class),context.services().require(VulcanService.class),context.events(),configured.fastCrystal(),clock);
        plugin.getServer().getPluginManager().registerEvents(crystalController,plugin);plugin.getServer().getOnlinePlayers().forEach(crystalController::refresh);
        context.services().register("combat",FastCrystalController.class,crystalController);context.placeholders().register("combat","fast_crystal",placeholder->placeholder.subject().map(crystalController::enabled).map(String::valueOf).orElse("false"));
        fastCrystalController.set(crystalController);
    },ignored->{CombatPlayerListener listener=combatPlayerListener.getAndSet(null);if(listener!=null)HandlerList.unregisterAll(listener);FastCrystalController crystal=fastCrystalController.getAndSet(null);if(crystal!=null)crystal.close();},()->HealthReport.healthy("combat"));}

    private MagicModule display(){String provider=config.integrations().display().provider().toUpperCase();ProviderMode mode=featureMode("display")==ProviderMode.DISABLED?ProviderMode.DISABLED
            :provider.equals("TAB")?ProviderMode.EXTERNAL:ProviderMode.INTERNAL;return new SimpleModule(new ModuleDescriptor("display",mode,Set.of("profiles","ranks","settings","statistics","lifesteal","economy")),context->{
        if(!Set.of("INTERNAL","TAB").contains(provider))return List.of("integrations.yml display.provider must be INTERNAL or TAB");
        if(provider.equals("TAB")&&plugin.getServer().getPluginManager().getPlugin("TAB")==null)return List.of("TAB display provider selected but TAB is unavailable");return List.of();},context->{
        DisplayService service;if(provider.equals("TAB"))service=new TabDisplayService();else{InternalDisplayService internal=new InternalDisplayService(plugin,scheduler,
                context.services().require(PlayerSettingsService.class),context.services().require(RankService.class),context.placeholders(),config.display(),config.ranks().definitions());
            plugin.getServer().getPluginManager().registerEvents(internal,plugin);internalDisplay.set(internal);service=internal;
            context.events().subscribe("display",com.magicstudios.magiccore.ranks.RankChanged.class,event->internal.refresh(event.playerId()));
            context.events().subscribe("display",com.magicstudios.magiccore.modules.statistics.StatsChanged.class,event->internal.refresh(event.playerId()));
            context.events().subscribe("display",com.magicstudios.magiccore.modules.economy.BalanceChanged.class,event->internal.refresh(event.playerId()));
            context.events().subscribe("display",com.magicstudios.magiccore.modules.lifesteal.HeartTransferred.class,event->{internal.refresh(event.killerId());internal.refresh(event.victimId());});}
        context.services().register("display",DisplayService.class,service);
    },ignored->{InternalDisplayService internal=internalDisplay.getAndSet(null);if(internal!=null){HandlerList.unregisterAll(internal);internal.close();}},()->HealthReport.healthy("display:"+provider));}

    private MagicModule marketplace(){return module("marketplace",ProviderMode.INTERNAL,Set.of("auctions","orders","bounties","ranks"),context->{
        MarketplaceAnalyticsService analytics=new PersistentMarketplaceAnalyticsService(store,clock,config.economy().definitions());
        context.services().register("marketplace",MarketplaceAnalyticsService.class,analytics);
        CapabilityService capabilities=context.services().require(CapabilityService.class);
        com.magicstudios.magiccore.admin.CapabilityGate gate=(actor,capability)->actor.console()
                ?java.util.concurrent.CompletableFuture.completedFuture(true):capabilities.has(actor.playerId(),capability);
        context.services().register("marketplace",MarketplaceAdminService.class,new DefaultMarketplaceAdminService(gate,analytics,
                context.services().require(AuctionService.class),context.services().require(OrderService.class),
                context.services().require(com.magicstudios.magiccore.audit.AuditService.class),clock));
    });}

    private MagicModule leaderboards(){return module("leaderboards",ProviderMode.INTERNAL,Set.of("statistics","lifesteal","marketplace"),context->{
        CompetitiveLeaderboardService service=new CachedCompetitiveLeaderboards(context.services().require(PlayerStatsService.class),context.services().require(LifestealService.class),
                context.services().require(MarketplaceAnalyticsService.class),clock,Duration.ofSeconds(config.display().leaderboardCacheSeconds()));context.services().register("leaderboards",CompetitiveLeaderboardService.class,service);
        context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.statistics.StatsChanged.class,event->service.invalidateStats());context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.lifesteal.HeartTransferred.class,event->service.invalidateHearts());
        context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.lifesteal.PlayerRevived.class,event->service.invalidateHearts());context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.economy.BalanceChanged.class,event->service.invalidateWealth());
        context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.crates.CrateOpened.class,event->service.invalidateWealth());context.events().subscribe("leaderboards",com.magicstudios.magiccore.modules.store.PurchaseFulfilled.class,event->service.invalidateWealth());
    });}

    private MagicModule admin() {
        return module("admin", ProviderMode.INTERNAL, Set.of(), context -> {
            context.services().register("admin", SetupService.class, new DefaultSetupService());
            context.services().register("admin", InputSessionService.class, new NativeInputSessionService(clock));
            var capabilities = context.services().find(CapabilityService.class);
            com.magicstudios.magiccore.admin.CapabilityGate gate = (actor, capability) -> actor.console()
                    ? java.util.concurrent.CompletableFuture.completedFuture(true)
                    : capabilities.map(service -> service.has(actor.playerId(), capability))
                    .orElseGet(() -> java.util.concurrent.CompletableFuture.completedFuture(false));
            context.services().register("admin", AdminEditingService.class,
                    new DefaultAdminEditingService(featuresStore, gate,
                            context.services().require(com.magicstudios.magiccore.audit.AuditService.class), clock));
        });
    }

    private MagicModule placeholders() {
        return new SimpleModule(new ModuleDescriptor("phase-one-placeholders", ProviderMode.INTERNAL,
                placeholderDependencies()), ignored -> List.of(), context -> {
                    PhaseOnePlaceholderView view = new PhaseOnePlaceholderView(
                            context.services().find(PlayerProfileService.class).orElse(null),
                            context.services().find(EconomyService.class).orElse(null),
                            context.services().find(RankService.class).orElse(null),
                            context.services().find(TeamService.class).orElse(null),
                            context.services().find(RewardService.class).orElse(null));
                    view.register("phase-one-placeholders", context.placeholders(), context.events());
                    context.services().register("phase-one-placeholders", PhaseOnePlaceholderView.class, view);
                    PhaseTwoPlaceholderView phaseTwo = null;
                    if (context.services().find(HomeService.class).isPresent()
                            && context.services().find(PlayerSettingsService.class).isPresent()
                            && context.services().find(PlayerWarpService.class).isPresent()) {
                        phaseTwo = new PhaseTwoPlaceholderView(context.services().require(HomeService.class),
                                context.services().require(PlayerSettingsService.class),
                                context.services().require(PlayerWarpService.class),
                                context.services().find(ShopService.class).orElse(null));
                        phaseTwo.register("phase-two-placeholders", context.placeholders());
                        context.services().register("phase-two-placeholders", PhaseTwoPlaceholderView.class, phaseTwo);
                    }
                    PhaseTwoPlaceholderView finalPhaseTwo = phaseTwo;
                    PhaseFourPlaceholderView phaseFour = null;
                    if (context.services().find(LifestealService.class).isPresent()
                            && context.services().find(CombatService.class).isPresent()
                            && context.services().find(NewPlayerProtectionService.class).isPresent()) {
                        phaseFour = new PhaseFourPlaceholderView(context.services().require(LifestealService.class),
                                context.services().require(CombatService.class),
                                context.services().require(NewPlayerProtectionService.class));
                        phaseFour.register("phase-one-placeholders", context.placeholders(), context.events());
                        context.services().register("phase-one-placeholders", PhaseFourPlaceholderView.class, phaseFour);
                    }
                    PhaseFourPlaceholderView finalPhaseFour = phaseFour;
                    PhaseFivePlaceholderView phaseFive = null;
                    if (context.services().find(CrateService.class).isPresent()) {
                        phaseFive = new PhaseFivePlaceholderView(context.services().require(CrateService.class),context.services().require(PlayerStatsService.class));
                        phaseFive.register("phase-one-placeholders", context.placeholders(), context.events());
                        context.services().register("phase-one-placeholders", PhaseFivePlaceholderView.class, phaseFive);
                    }
                    PhaseFivePlaceholderView finalPhaseFive = phaseFive;
                    if(context.services().find(com.magicstudios.magiccore.modules.events.KothService.class).isPresent()&&context.services().find(com.magicstudios.magiccore.modules.events.VotePartyService.class).isPresent()){
                        var phaseSeven=new com.magicstudios.magiccore.placeholders.PhaseSevenPlaceholderView(context.services().require(com.magicstudios.magiccore.modules.events.KothService.class),context.services().require(com.magicstudios.magiccore.modules.events.VotePartyService.class));phaseSeven.register("phase-one-placeholders",context.placeholders(),context.events());phaseSeven.refresh();context.services().register("phase-one-placeholders",com.magicstudios.magiccore.placeholders.PhaseSevenPlaceholderView.class,phaseSeven);
                    }
                    context.services().find(PlayerProfileService.class).ifPresent(profileService -> {
                        PhaseOnePlayerListener listener = new PhaseOnePlayerListener(profileService, view, finalPhaseTwo, finalPhaseFour, finalPhaseFive);
                        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                        playerListener.set(listener);
                    });
                }, ignored -> {
                    PhaseOnePlayerListener listener = playerListener.getAndSet(null);
                    if (listener != null) HandlerList.unregisterAll(listener);
                }, () -> HealthReport.healthy("phase-one-placeholders"));
    }

    private MagicModule vault() {
        boolean enabled = featureMode("economy") == ProviderMode.INTERNAL
                && config.integrations().vault().enabled() && config.integrations().vault().registerInternalEconomy();
        return new SimpleModule(new ModuleDescriptor("vault-integration", enabled ? ProviderMode.EXTERNAL : ProviderMode.DISABLED,
                Set.of("economy")), context -> List.of(), context -> {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
            AutoCloseable registration = VaultIntegrationBridge.registerInternal(plugin,
                    context.services().require(EconomyService.class), context.events());
            vault.set(registration);
        }, ignored -> {
            close(vault.getAndSet(null));
        }, () -> plugin.getServer().getPluginManager().getPlugin("Vault") == null
                ? new HealthReport("integration:Vault", HealthState.AVAILABLE, "Vault is not installed; internal economy remains authoritative", Map.of(), java.time.Instant.now())
                : new HealthReport("integration:Vault", HealthState.DEGRADED,
                "Vault's synchronous API is active for cached reads; tick-thread mutations are rejected to preserve the no-blocking-I/O guarantee",
                Map.of("safe-alternative", "MagicCore EconomyService async API"), java.time.Instant.now()));
    }

    private MagicModule papi() {
        boolean enabled = config.integrations().placeholderapi().enabled();
        return new SimpleModule(new ModuleDescriptor("placeholderapi-integration", enabled ? ProviderMode.EXTERNAL : ProviderMode.DISABLED,
                Set.of("phase-one-placeholders")), context -> List.of(), context -> {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return;
            AutoCloseable expansion = PlaceholderApiIntegrationBridge.register(plugin, context.placeholders());
            papi.set(expansion);
        }, ignored -> {
            close(papi.getAndSet(null));
        }, () -> plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null
                ? new HealthReport("integration:PlaceholderAPI", HealthState.AVAILABLE,
                "PlaceholderAPI is not installed; internal placeholders remain available", Map.of(), java.time.Instant.now())
                : HealthReport.healthy("integration:PlaceholderAPI"));
    }

    private MagicModule module(String id, ProviderMode mode, Set<String> dependencies, java.util.function.Consumer<ModuleContext> start) {
        return new SimpleModule(new ModuleDescriptor(id, mode, dependencies), ignored -> List.of(), start,
                ignored -> { }, () -> HealthReport.healthy(id));
    }

    private ProviderMode featureMode(String id) {
        if (id.equals("koth") && !config.events().koth().enabled()) return ProviderMode.DISABLED;
        if (id.equals("vote-party") && !config.events().voteParty().enabled()) return ProviderMode.DISABLED;
        if (id.equals("gemshop") && !config.shop().gemShop().enabled()) return ProviderMode.DISABLED;
        return config.features().features().getOrDefault(id, ProviderMode.DISABLED);
    }

    private Set<String> placeholderDependencies() {
        return java.util.stream.Stream.of("profiles", "ranks", "economy", "teams", "rewards", "settings", "essentials", "player-warps", "shop", "lifesteal", "combat", "crates", "statistics", "koth", "vote-party")
                .filter(id -> id.equals("statistics") || (id.equals("ranks") ? configuredRankMode() != ProviderMode.DISABLED
                        : featureMode(id) != ProviderMode.DISABLED)
                )
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private ProviderMode configuredRankMode() {
        return featureMode("ranks") == ProviderMode.DISABLED ? ProviderMode.DISABLED
                : ProviderMode.valueOf(config.ranks().system().provider().toUpperCase());
    }

    private static void close(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception failure) {
            throw new IllegalStateException("Integration shutdown failed", failure);
        }
    }
}
