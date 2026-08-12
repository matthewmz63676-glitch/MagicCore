package com.magicstudios.magiccore.config;

import com.magicstudios.magiccore.config.model.EconomyFile;
import com.magicstudios.magiccore.config.model.CoreFile;
import com.magicstudios.magiccore.config.model.FeaturesFile;
import com.magicstudios.magiccore.config.model.IntegrationsFile;
import com.magicstudios.magiccore.config.model.RanksFile;
import com.magicstudios.magiccore.config.model.RewardsFile;
import com.magicstudios.magiccore.config.model.StorageFile;
import com.magicstudios.magiccore.config.model.TeamsFile;
import com.magicstudios.magiccore.config.model.EssentialsFile;
import com.magicstudios.magiccore.config.model.ShopFile;
import com.magicstudios.magiccore.config.model.SettingsFile;
import com.magicstudios.magiccore.config.model.AuctionFile;
import com.magicstudios.magiccore.config.model.OrdersFile;
import com.magicstudios.magiccore.config.model.BountiesFile;
import com.magicstudios.magiccore.config.model.LifestealFile;
import com.magicstudios.magiccore.config.model.CombatFile;
import com.magicstudios.magiccore.config.model.CratesFile;
import com.magicstudios.magiccore.config.model.DisplayFile;
import com.magicstudios.magiccore.config.model.StoreFile;
import com.magicstudios.magiccore.config.model.AfkFile;
import com.magicstudios.magiccore.config.model.PresentationFile;
import com.magicstudios.magiccore.config.model.SpawnStashFile;
import com.magicstudios.magiccore.config.model.ItemWorthFile;
import com.magicstudios.magiccore.config.model.BillfordFile;
import com.magicstudios.magiccore.config.model.ToolsFile;
import com.magicstudios.magiccore.config.model.SecureStorageFile;
import com.magicstudios.magiccore.config.model.DiscordBridgeFile;
import com.magicstudios.magiccore.config.model.PlayerWarpsFile;
import com.magicstudios.magiccore.config.model.MenusFile;
import com.magicstudios.magiccore.config.model.EventsFile;
import com.magicstudios.magiccore.modules.teams.TeamNamePolicy;
import com.magicstudios.magiccore.ranks.RankCatalog;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicCoreConfigurationLoader {
    public static final List<String> DEFAULT_RESOURCES = List.of("config.yml", "features.yml", "integrations.yml",
            "storage.yml", "ranks.yml", "messages.yml", "modules/economy.yml", "modules/teams.yml", "modules/rewards.yml",
            "modules/essentials.yml", "modules/shop.yml", "modules/settings.yml", "modules/auction.yml", "modules/orders.yml",
            "modules/bounties.yml", "modules/lifesteal.yml", "modules/combat.yml", "modules/crates.yml", "modules/display.yml", "modules/store.yml", "modules/afk.yml", "modules/presentation.yml", "modules/spawnstash.yml", "modules/item-worth.yml", "modules/billford.yml", "modules/tools.yml", "modules/secure-storage.yml", "modules/discord-bridge.yml", "modules/playerwarps.yml", "modules/menus.yml", "modules/events.yml");

    private final Path dataDirectory;
    private final java.util.function.Function<String, InputStream> resources;

    public MagicCoreConfigurationLoader(Path dataDirectory, java.util.function.Function<String, InputStream> resources) {
        this.dataDirectory = dataDirectory;
        this.resources = resources;
    }

    public MagicCoreConfiguration installAndLoad() throws Exception {
        for (String resource : DEFAULT_RESOURCES) install(resource);
        CoreFile core = read("config.yml", CoreFile.class);
        FeaturesFile features = read("features.yml", FeaturesFile.class);
        IntegrationsFile integrations = read("integrations.yml", IntegrationsFile.class);
        StorageFile storage = read("storage.yml", StorageFile.class);
        RanksFile ranks = read("ranks.yml", RanksFile.class);
        EconomyFile economy = read("modules/economy.yml", EconomyFile.class);
        TeamsFile teams = read("modules/teams.yml", TeamsFile.class);
        RewardsFile rewards = read("modules/rewards.yml", RewardsFile.class);
        EssentialsFile essentials = read("modules/essentials.yml", EssentialsFile.class);
        ShopFile shop = read("modules/shop.yml", ShopFile.class);
        SettingsFile settings = read("modules/settings.yml", SettingsFile.class);
        AuctionFile auction = read("modules/auction.yml", AuctionFile.class);
        OrdersFile orders = read("modules/orders.yml", OrdersFile.class);
        BountiesFile bounties = read("modules/bounties.yml", BountiesFile.class);
        LifestealFile lifesteal = read("modules/lifesteal.yml", LifestealFile.class);
        CombatFile combat = read("modules/combat.yml", CombatFile.class);
        CratesFile crates = read("modules/crates.yml", CratesFile.class);
        DisplayFile display = read("modules/display.yml", DisplayFile.class);
        StoreFile store = read("modules/store.yml", StoreFile.class);
        AfkFile afk = read("modules/afk.yml", AfkFile.class);
        PresentationFile presentation = read("modules/presentation.yml", PresentationFile.class);
        SpawnStashFile spawnStash = read("modules/spawnstash.yml", SpawnStashFile.class);
        ItemWorthFile itemWorth = read("modules/item-worth.yml", ItemWorthFile.class);
        BillfordFile billford = read("modules/billford.yml", BillfordFile.class);
        ToolsFile tools = read("modules/tools.yml", ToolsFile.class);
        SecureStorageFile secureStorage = read("modules/secure-storage.yml", SecureStorageFile.class);
        DiscordBridgeFile discordBridge = read("modules/discord-bridge.yml", DiscordBridgeFile.class);
        PlayerWarpsFile playerWarps = read("modules/playerwarps.yml", PlayerWarpsFile.class);
        MenusFile menus = read("modules/menus.yml", MenusFile.class);
        EventsFile events = read("modules/events.yml", EventsFile.class);
        Map<String, String> messages = flattenMessages(dataDirectory.resolve("messages.yml"));
        MagicCoreConfiguration configuration = new MagicCoreConfiguration(core, features, integrations, storage,
                ranks, economy, teams, rewards, essentials, shop, settings, auction, orders, bounties, lifesteal, combat, crates, display, store, afk, presentation, spawnStash, itemWorth, billford, tools, secureStorage, discordBridge, playerWarps, menus, events, messages);
        validate(configuration);
        return configuration;
    }

    private <T> T read(String relative, Class<T> type) throws Exception {
        return new YamlConfigCodec<>(type).decode(Files.readAllBytes(dataDirectory.resolve(relative)));
    }

    private void install(String relative) throws Exception {
        Path target = dataDirectory.resolve(relative);
        if (Files.exists(target)) return;
        Files.createDirectories(target.getParent());
        try (InputStream source = resources.apply(relative)) {
            if (source == null) throw new IllegalStateException("Bundled resource is missing: " + relative);
            Files.copy(source, target);
        }
    }

    private static void validate(MagicCoreConfiguration configuration) {
        if (configuration.core().configVersion() != 1 || configuration.features().configVersion() != 1 || configuration.integrations().configVersion()!=1 || configuration.storage().configVersion() != 1
                || configuration.ranks().configVersion() != 1 || configuration.economy().configVersion() != 1
                || configuration.teams().configVersion() != 1 || configuration.rewards().configVersion() != 1
                || configuration.essentials().configVersion() != 1 || configuration.shop().configVersion() != 1
                || configuration.settings().configVersion() != 1) {
            throw new IllegalArgumentException("All configuration files must use config-version: 1");
        }
        if (configuration.auction().configVersion() != 1) throw new IllegalArgumentException("modules/auction.yml config-version must be 1");
        if (configuration.orders().configVersion() != 1) throw new IllegalArgumentException("modules/orders.yml config-version must be 1");
        if (configuration.bounties().configVersion() != 1) throw new IllegalArgumentException("modules/bounties.yml config-version must be 1");
        if (configuration.lifesteal().configVersion() != 1) throw new IllegalArgumentException("modules/lifesteal.yml config-version must be 1");
        if (configuration.combat().configVersion() != 1) throw new IllegalArgumentException("modules/combat.yml config-version must be 1");
        if (configuration.crates().configVersion() != 1) throw new IllegalArgumentException("modules/crates.yml config-version must be 1");
        if (configuration.display().configVersion() != 1) throw new IllegalArgumentException("modules/display.yml config-version must be 1");
        if (configuration.store().configVersion() != 1) throw new IllegalArgumentException("modules/store.yml config-version must be 1");
        if (configuration.afk().configVersion() != 1) throw new IllegalArgumentException("modules/afk.yml config-version must be 1");
        if (configuration.presentation().configVersion() != 1) throw new IllegalArgumentException("modules/presentation.yml config-version must be 1");
        validatePresentation(configuration.presentation());
        if (configuration.spawnStash().configVersion() != 1) throw new IllegalArgumentException("modules/spawnstash.yml config-version must be 1");
        validateSpawnStash(configuration.spawnStash());
        if(configuration.itemWorth().configVersion()!=1)throw new IllegalArgumentException("modules/item-worth.yml config-version must be 1");
        validateItemWorth(configuration.itemWorth(),configuration.economy());
        if(configuration.billford().configVersion()!=1)throw new IllegalArgumentException("modules/billford.yml config-version must be 1");validateBillford(configuration.billford(),configuration.economy());
        if(configuration.tools().configVersion()!=1)throw new IllegalArgumentException("modules/tools.yml config-version must be 1");validateTools(configuration.tools());
        if(configuration.secureStorage().configVersion()!=1)throw new IllegalArgumentException("modules/secure-storage.yml config-version must be 1");validateSecureStorage(configuration.secureStorage());
        if(configuration.discordBridge().configVersion()!=1)throw new IllegalArgumentException("modules/discord-bridge.yml config-version must be 1");validateDiscordBridge(configuration.discordBridge());
        if(configuration.playerWarps().configVersion()!=1)throw new IllegalArgumentException("modules/playerwarps.yml config-version must be 1");validatePlayerWarps(configuration.playerWarps(),configuration.economy());
        if(configuration.menus().configVersion()!=1)throw new IllegalArgumentException("modules/menus.yml config-version must be 1");validateMenus(configuration.menus());validateMenuText(configuration.menus());
        if(configuration.events().configVersion()!=1)throw new IllegalArgumentException("modules/events.yml config-version must be 1");validateEvents(configuration.events(),configuration.economy());
        validateIntegrationProviders(configuration.integrations());
        if (configuration.core().io().threads() < 1 || configuration.core().io().queueCapacity() < 1) {
            throw new IllegalArgumentException("config.yml io threads and queue-capacity must be positive");
        }
        new RankCatalog(configuration.ranks().system().defaultRank(), configuration.ranks().definitions());
        configuration.settings().defaults().forEach((key,value)->{try{com.magicstudios.magiccore.modules.settings.PlayerSetting.valueOf(key);}catch(IllegalArgumentException failure){throw new IllegalArgumentException("modules/settings.yml contains unknown setting "+key,failure);}});
        if (!configuration.economy().definitions().containsKey(configuration.economy().primaryCurrency())) {
            throw new IllegalArgumentException("modules/economy.yml: primary-currency must reference a configured currency");
        }
        TeamsFile.NamePolicy policy = configuration.teams().namePolicy();
        new TeamNamePolicy(policy.minimumLength(), policy.maximumLength(), policy.pattern(), policy.blockedNames());
        Set<String> rewardIds = new LinkedHashSet<>();
        configuration.rewards().daily().definitions().forEach(reward -> {
            if (!rewardIds.add(reward.id())) throw new IllegalArgumentException("Duplicate reward ID: " + reward.id());
            if (!configuration.economy().definitions().containsKey(reward.currency()))
                throw new IllegalArgumentException("Reward " + reward.id() + " references unknown currency " + reward.currency());
        });
        configuration.rewards().playtime().definitions().forEach(reward -> {
            if (!rewardIds.add(reward.id())) throw new IllegalArgumentException("Duplicate reward ID: " + reward.id());
            if (!configuration.economy().definitions().containsKey(reward.currency()))
                throw new IllegalArgumentException("Milestone " + reward.id() + " references unknown currency " + reward.currency());
        });
        if (!configuration.economy().definitions().containsKey(configuration.shop().currency())) {
            throw new IllegalArgumentException("modules/shop.yml references unknown currency " + configuration.shop().currency());
        }
        var gemShop=configuration.shop().gemShop();
        if(gemShop==null||!configuration.economy().definitions().containsKey(gemShop.currency())||gemShop.confirmationSeconds()<1||gemShop.confirmationSeconds()>300)
            throw new IllegalArgumentException("modules/shop.yml gem-shop policy is invalid");
        Set<String>gemProductIds=new java.util.HashSet<>();
        for(var product:gemShop.products())if(product.id()==null||!product.id().matches("[A-Z][A-Z0-9_]*")||!gemProductIds.add(product.id())
                ||product.category()==null||!product.category().matches("[A-Z][A-Z0-9_]*")||product.displayName()==null||product.displayName().isBlank()
                ||product.material()==null||!product.material().matches("[A-Z0-9_]+")||product.amount()<1||product.amount()>64||product.priceMinor()<1
                ||product.minimumPlaytimeSeconds()<0||product.minimumKills()<0||(product.requiredCapability()!=null&&!product.requiredCapability().isBlank()&&!product.requiredCapability().matches("[A-Z][A-Z0-9_]*")))
            throw new IllegalArgumentException("modules/shop.yml contains an invalid gem product");
        if (!configuration.economy().definitions().containsKey(configuration.auction().currency()))
            throw new IllegalArgumentException("modules/auction.yml references unknown currency " + configuration.auction().currency());
        if (!configuration.economy().definitions().containsKey(configuration.orders().currency()))
            throw new IllegalArgumentException("modules/orders.yml references unknown currency " + configuration.orders().currency());
        if (!configuration.economy().definitions().containsKey(configuration.bounties().currency()))
            throw new IllegalArgumentException("modules/bounties.yml references unknown currency " + configuration.bounties().currency());
        if (configuration.auction().minimumPriceMinor() < 1
                || configuration.auction().maximumPriceMinor() < configuration.auction().minimumPriceMinor()
                || configuration.auction().listingFeeMinor() < 0
                || configuration.auction().minimumDurationSeconds() < 1
                || configuration.auction().maximumDurationSeconds() < configuration.auction().minimumDurationSeconds())
            throw new IllegalArgumentException("modules/auction.yml bounds are invalid");
        if (configuration.auction().categories().isEmpty()
                || configuration.auction().categories().stream().anyMatch(id -> !id.matches("[a-z0-9_-]{1,24}"))
                || new java.util.HashSet<>(configuration.auction().categories()).size() != configuration.auction().categories().size())
            throw new IllegalArgumentException("modules/auction.yml categories must be unique safe IDs");
        if(configuration.orders().minimumUnitPriceMinor()<1||configuration.orders().maximumUnitPriceMinor()<configuration.orders().minimumUnitPriceMinor()
                ||configuration.orders().minimumDurationSeconds()<1||configuration.orders().maximumDurationSeconds()<configuration.orders().minimumDurationSeconds()
                ||configuration.orders().categories().isEmpty())throw new IllegalArgumentException("modules/orders.yml bounds are invalid");
        if(configuration.bounties().minimumAmountMinor()<1||configuration.bounties().maximumAmountMinor()<configuration.bounties().minimumAmountMinor()
                ||configuration.bounties().taxBasisPoints()<0||configuration.bounties().taxBasisPoints()>10_000
                ||configuration.bounties().maximumContributionsPerTarget()<1||configuration.bounties().maximumContributionsPerTarget()>1000
                ||configuration.bounties().restrictions().minimumTargetKills()<0||configuration.bounties().restrictions().minimumTargetPlaytimeSeconds()<0)
            throw new IllegalArgumentException("modules/bounties.yml policy is invalid");
        var hearts=configuration.lifesteal();
        if(hearts.minimumHearts()<1||hearts.startingHearts()<hearts.minimumHearts()||hearts.maximumHearts()<hearts.startingHearts()
                ||hearts.revivalHearts()<hearts.minimumHearts()||hearts.revivalHearts()>hearts.maximumHearts()
                ||hearts.samePlayerCooldownSeconds()<0)throw new IllegalArgumentException("modules/lifesteal.yml heart policy is invalid");
        try{com.magicstudios.magiccore.modules.lifesteal.PersistentLifestealService.NonPlayerDeathPolicy.valueOf(hearts.nonPlayerDeathPolicy());}
        catch(IllegalArgumentException failure){throw new IllegalArgumentException("modules/lifesteal.yml non-player-death-policy is invalid",failure);}
        if(!java.util.Set.of("SPECTATOR","KICK").contains(hearts.eliminationAction()))throw new IllegalArgumentException("modules/lifesteal.yml elimination-action is invalid");
        if((hearts.recipe().enabled()&&(hearts.recipe().shape().size()!=3||hearts.recipe().shape().stream().anyMatch(row->row.length()!=3)))
                ||(hearts.revivalRecipe().enabled()&&(hearts.revivalRecipe().shape().size()!=3||hearts.revivalRecipe().shape().stream().anyMatch(row->row.length()!=3))))
            throw new IllegalArgumentException("modules/lifesteal.yml recipes must use 3x3 shapes");
        if(!hearts.heartItem().material().matches("[A-Z0-9_]+")||!hearts.revivalItem().material().matches("[A-Z0-9_]+")
                ||java.util.stream.Stream.of(hearts.recipe(),hearts.revivalRecipe()).flatMap(recipe->recipe.ingredients().entrySet().stream())
                .anyMatch(entry->entry.getKey().length()!=1||!entry.getValue().matches("[A-Z0-9_]+")))
            throw new IllegalArgumentException("modules/lifesteal.yml item materials/recipe keys are invalid");
        var combat=configuration.combat();if(combat.tagDurationSeconds()<1||combat.enderPearlCooldownMillis()<0||combat.tridentCooldownMillis()<0
                ||combat.newbieProtection().durationSeconds()<0||!combat.logoutPolicy().equals("KILL_AND_CREDIT")
                ||combat.restrictedCommands().stream().anyMatch(command->!command.matches("[a-z0-9_-]+"))
                ||combat.restrictedItems().stream().anyMatch(material->!material.matches("[A-Z0-9_]+")))
            throw new IllegalArgumentException("modules/combat.yml policy is invalid");
        var crystal=combat.fastCrystal();
        if(crystal==null||crystal.cooldownMillis()<0||crystal.maximumRange()<=0||crystal.maximumRange()>32
                ||crystal.damage()<0||crystal.damage()>100||crystal.knockback()<0||crystal.knockback()>10
                ||crystal.soundVolume()<0||crystal.soundPitch()<0||crystal.sound()==null||!crystal.sound().matches("[A-Z0-9_]+")
                ||crystal.worldAllowlist().stream().anyMatch(value->value==null||value.isBlank())
                ||crystal.baseBlocks().isEmpty()||crystal.baseBlocks().stream().anyMatch(value->value==null||!value.matches("[A-Z0-9_]+")))
            throw new IllegalArgumentException("modules/combat.yml fast-crystal policy is invalid");
        var crates=configuration.crates();if(!configuration.economy().definitions().containsKey(crates.currency()))
            throw new IllegalArgumentException("modules/crates.yml cost currency is unknown");
        if(crates.keyall()==null||crates.keyall().maximumRecipients()<1||crates.keyall().maximumRecipients()>100_000)
            throw new IllegalArgumentException("modules/crates.yml keyall recipient bounds are invalid");
        Set<String>keyallIds=new java.util.HashSet<>();
        for(var definition:crates.keyall().definitions())if(definition.id()==null||!definition.id().matches("[A-Z][A-Z0-9_]*")||!keyallIds.add(definition.id())
                ||definition.keyId()==null||definition.keyId().isBlank()||definition.amount()<1||!Set.of("ONLINE","ALL_KNOWN").contains(definition.audience())
                ||definition.scheduleIntervalSeconds()<0||definition.threshold()<0)
            throw new IllegalArgumentException("modules/crates.yml contains an invalid keyall definition");
        java.util.Set<String>crateIds=new java.util.HashSet<>();for(var crate:crates.crates()){
            if(!crateIds.add(crate.id())||!crate.id().matches("[A-Z][A-Z0-9_]*")||crate.maximumOpenAmount()<1||crate.rewards().isEmpty())
                throw new IllegalArgumentException("modules/crates.yml crate definition is invalid: "+crate.id());
            try{com.magicstudios.magiccore.modules.crates.CrateCost.Type.valueOf(crate.cost().type());}catch(IllegalArgumentException failure){throw new IllegalArgumentException("Unknown crate cost type",failure);}
            if(crate.cost().amount()<1)throw new IllegalArgumentException("Crate cost must be positive");
            java.util.Set<String>crateRewardIds=new java.util.HashSet<>();for(var reward:crate.rewards()){
                if(!crateRewardIds.add(reward.id())||reward.weight()<1)throw new IllegalArgumentException("Invalid crate reward "+reward.id());
                try{com.magicstudios.magiccore.modules.crates.CrateReward.Type.valueOf(reward.type());}catch(IllegalArgumentException failure){throw new IllegalArgumentException("Unknown crate reward type",failure);}
                if(reward.type().equals("CURRENCY")&&!configuration.economy().definitions().containsKey(reward.currency()))throw new IllegalArgumentException("Unknown crate reward currency "+reward.currency());
                if(reward.type().equals("ITEM")&&!reward.material().matches("[A-Z0-9_]+"))throw new IllegalArgumentException("Invalid crate reward material");
            }
            for(var milestone:crate.milestones())if(milestone.openCount()<1||!crateRewardIds.contains(milestone.rewardId()))throw new IllegalArgumentException("Invalid crate milestone");
        }
        var teleport = configuration.essentials().teleport();
        if (teleport.requestLifetimeSeconds() < 1 || teleport.warmupSeconds() < 0 || teleport.movementTolerance() < 0
                || teleport.cooldownSeconds() < 0 || teleport.costMinor() < 0)
            throw new IllegalArgumentException("modules/essentials.yml teleport values are invalid");
        if (!configuration.economy().definitions().containsKey(teleport.currency()))
            throw new IllegalArgumentException("modules/essentials.yml teleport currency is unknown: " + teleport.currency());
        var rtp = configuration.essentials().rtp();
        new com.magicstudios.magiccore.modules.essentials.RtpBounds(rtp.centerX(), rtp.centerZ(),
                rtp.minimumRadius(), rtp.maximumRadius(), rtp.maximumAttempts());
        MiniMessageRenderer renderer = new MiniMessageRenderer();
        renderer.validateCatalog(configuration.messages());
        renderer.validate(configuration.lifesteal().heartItem().displayName());
        renderer.validate(configuration.lifesteal().revivalItem().displayName());
        configuration.crates().crates().forEach(crate->renderer.validate(crate.displayName()));
        configuration.shop().gemShop().products().forEach(product->renderer.validate(product.displayName()));
        var display=configuration.display();if(display.refreshSeconds()<1||display.leaderboardCacheSeconds()<1||display.scoreboard().lines().size()>15)
            throw new IllegalArgumentException("modules/display.yml refresh/cache/line bounds are invalid");
        renderer.validate(display.scoreboard().title());display.scoreboard().lines().forEach(renderer::validate);
        renderer.validate(display.tab().header());renderer.validate(display.tab().footer());renderer.validate(display.tab().nameFormat());
        renderer.validate(display.belowName().label());renderer.validate(display.chat().format().replace("<message>","message"));
        var store=configuration.store();if(store.url().isBlank()||store.signatureMaximumAgeSeconds()<1||store.donationGoal().targetMinor()<1)throw new IllegalArgumentException("modules/store.yml policy is invalid");
        java.util.Set<String>productIds=new java.util.HashSet<>();for(var product:store.products()){
            if(!productIds.add(product.id())||!product.id().matches("[A-Z][A-Z0-9_]*")||product.minimumPaidMinor()<0||product.actions().isEmpty())throw new IllegalArgumentException("Invalid store product "+product.id());
            renderer.validate(product.displayName());for(var action:product.actions()){com.magicstudios.magiccore.modules.store.ProductAction.Type type;
                try{type=com.magicstudios.magiccore.modules.store.ProductAction.Type.valueOf(action.type());}catch(IllegalArgumentException failure){throw new IllegalArgumentException("Unknown product action type",failure);}switch(type){
                    case CURRENCY->{if(action.amountMinor()<1||!configuration.economy().definitions().containsKey(action.currency()))throw new IllegalArgumentException("Invalid product currency action");}
                    case CRATE_KEY->{if(action.keyId().isBlank()||action.keyAmount()<1)throw new IllegalArgumentException("Invalid product crate-key action");}
                    case ITEM->{if(action.amount()<1||!action.material().matches("[A-Z0-9_]+"))throw new IllegalArgumentException("Invalid product item action");}
                    case RANK->{if(!configuration.ranks().definitions().containsKey(action.rankId()))throw new IllegalArgumentException("Invalid product rank action");}}}
        }
        configuration.ranks().definitions().forEach((id, rank) -> renderer.validate(rank.display()));
        validateAfk(configuration.afk());
    }

    private static void validatePresentation(PresentationFile presentation) {
        java.util.Set<String> navigationIds = new java.util.HashSet<>();
        java.util.Set<Integer> infoSlots = new java.util.HashSet<>();
        java.util.Set<Integer> serverSlots = new java.util.HashSet<>();
        validateNavigation("info", presentation.info(), navigationIds, infoSlots);
        navigationIds.clear();
        validateNavigation("server-navigation", presentation.serverNavigation(), navigationIds, serverSlots);
        if (presentation.applications() == null || presentation.applications().media() == null || presentation.applications().staff() == null)
            throw new IllegalArgumentException("modules/presentation.yml must define media and staff applications");
        validateApplication("media", presentation.applications().media());
        validateApplication("staff", presentation.applications().staff());
    }

    private static void validateSpawnStash(SpawnStashFile config) {
        var placement = config.placement(); var signals = config.signals();
        if (!config.observeOnly()) throw new IllegalArgumentException("modules/spawnstash.yml must remain observe-only");
        if (config.expirySeconds() < 60 || config.expirySeconds() > 86_400 || placement.blockCount() < 1 || placement.blockCount() > 128
                || placement.minimumHorizontalRadius() < 1 || placement.maximumHorizontalRadius() < placement.minimumHorizontalRadius()
                || placement.maximumHorizontalRadius() > 256 || placement.minimumVerticalOffset() < -64
                || placement.maximumVerticalOffset() < placement.minimumVerticalOffset() || placement.maximumVerticalOffset() > 64
                || placement.clusterRadius() < 1 || placement.clusterRadius() > 16 || placement.maximumCandidateAttempts() < placement.blockCount()
                || placement.maximumCandidateAttempts() > 4096)
            throw new IllegalArgumentException("modules/spawnstash.yml placement/expiry bounds are invalid");
        if (signals.approachDistance() <= 0 || signals.revealDistance() < signals.approachDistance()
                || signals.revealDotProduct() < -1 || signals.revealDotProduct() > 1 || signals.suspiciousPathImprovement() <= 0
                || signals.suspiciousPathSamples() < 2 || signals.suspiciousPathSamples() > 100
                || signals.perSignalCooldownSeconds() < 1 || signals.perSignalCooldownSeconds() > 3600)
            throw new IllegalArgumentException("modules/spawnstash.yml signal policy is invalid");
        if (config.alerts().enabled() && (config.alerts().capability() == null || !config.alerts().capability().matches("[A-Z][A-Z0-9_]*")))
            throw new IllegalArgumentException("modules/spawnstash.yml alerts require a capability");
        if (config.decoyBlocks().isEmpty()) throw new IllegalArgumentException("modules/spawnstash.yml needs decoy blocks");
        Set<String> ids = new java.util.HashSet<>(); long totalWeight = 0;
        for (var block : config.decoyBlocks()) {
            if (block.id() == null || !block.id().matches("[a-z0-9_-]{1,32}") || !ids.add(block.id())
                    || block.blockData() == null || block.blockData().isBlank() || block.weight() < 1)
                throw new IllegalArgumentException("modules/spawnstash.yml has an invalid decoy block");
            totalWeight += block.weight();
            for (var loot : block.lootAppearance()) if (loot.material() == null || !loot.material().matches("[A-Z0-9_]+")
                    || loot.minimumAmount() < 1 || loot.maximumAmount() < loot.minimumAmount() || loot.maximumAmount() > 64 || loot.weight() < 1)
                throw new IllegalArgumentException("modules/spawnstash.yml has invalid loot appearance");
        }
        if (totalWeight > Integer.MAX_VALUE) throw new IllegalArgumentException("modules/spawnstash.yml total decoy weight is too large");
    }

    private static void validateItemWorth(ItemWorthFile config,EconomyFile economy){if(!economy.definitions().containsKey(config.currency()))throw new IllegalArgumentException("modules/item-worth.yml references unknown currency");
        var policies=config.policies();if(!Set.of("IGNORE","ADDITIVE","REJECT").contains(policies.enchantments().toUpperCase(java.util.Locale.ROOT))||policies.enchantmentBasisPointsPerLevel()<0||policies.enchantmentBasisPointsPerLevel()>100_000
                ||!Set.of("IGNORE","LINEAR","REJECT_DAMAGED").contains(policies.durability().toUpperCase(java.util.Locale.ROOT))||policies.minimumDurabilityBasisPoints()<0||policies.minimumDurabilityBasisPoints()>10_000
                ||!Set.of("ALLOW","REJECT_NONSTANDARD").contains(policies.metadata().toUpperCase(java.util.Locale.ROOT))||!Set.of("REJECT_NONEMPTY","REJECT_ALL","IGNORE").contains(policies.containers().toUpperCase(java.util.Locale.ROOT))
                ||!Set.of("REJECT_ALL","USE_ENTRY").contains(policies.spawners().toUpperCase(java.util.Locale.ROOT)))throw new IllegalArgumentException("modules/item-worth.yml policies are invalid");
        if(config.presentation().worthTemplate()==null||!config.presentation().worthTemplate().contains("{amount}")||config.presentation().unavailableText()==null||config.presentation().unavailableText().isBlank())throw new IllegalArgumentException("modules/item-worth.yml presentation is invalid");
        Set<String>ids=new java.util.HashSet<>(),itemIds=new java.util.HashSet<>();if(config.entries().isEmpty())throw new IllegalArgumentException("modules/item-worth.yml entries must not be empty");for(var entry:config.entries())if(entry.id()==null||!entry.id().matches("[a-z0-9_-]{1,32}")||!ids.add(entry.id())||entry.itemId()==null||!entry.itemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!itemIds.add(entry.itemId())||entry.category()==null||!entry.category().matches("[a-z0-9_-]{1,32}")||entry.unitWorthMinor()<1)throw new IllegalArgumentException("modules/item-worth.yml contains an invalid or duplicate entry");
        for(String id:policies.protectedItemIds())if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("modules/item-worth.yml protected item IDs are invalid");for(String key:policies.protectedMetadataKeys())if(!key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("modules/item-worth.yml protected metadata keys are invalid");}
    private static void validateBillford(BillfordFile config,EconomyFile economy){if(config.recipes().isEmpty())throw new IllegalArgumentException("modules/billford.yml recipes must not be empty");Set<String>recipeIds=new java.util.HashSet<>();for(var recipe:config.recipes()){if(recipe.id()==null||!recipe.id().matches("[a-z0-9_-]{1,32}")||!recipeIds.add(recipe.id())||recipe.displayName()==null||recipe.displayName().isBlank()||recipe.stock()<1||recipe.perPlayerLimit()<1||recipe.cooldownSeconds()<0||recipe.ingredients().isEmpty()||recipe.rewards().isEmpty())throw new IllegalArgumentException("modules/billford.yml recipe is invalid");Set<String>ingredients=new java.util.HashSet<>();for(var ingredient:recipe.ingredients())if(ingredient.itemId()==null||!ingredient.itemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!ingredients.add(ingredient.itemId())||ingredient.amount()<1)throw new IllegalArgumentException("modules/billford.yml ingredient is invalid");Set<String>rewards=new java.util.HashSet<>();long weight=0;for(var reward:recipe.rewards()){if(reward.id()==null||!reward.id().matches("[a-z0-9_-]{1,32}")||!rewards.add(reward.id())||reward.weight()<1)throw new IllegalArgumentException("modules/billford.yml reward is invalid");weight+=reward.weight();if(reward.type().equals("CURRENCY")&&(!economy.definitions().containsKey(reward.currency())||reward.amountMinor()<1))throw new IllegalArgumentException("modules/billford.yml currency reward is invalid");if(reward.type().equals("ITEM")&&(!reward.material().matches("[A-Z0-9_]+")||reward.amount()<1||reward.amount()>64))throw new IllegalArgumentException("modules/billford.yml item reward is invalid");if(!Set.of("CURRENCY","ITEM").contains(reward.type()))throw new IllegalArgumentException("modules/billford.yml reward type is invalid");}if(weight>Integer.MAX_VALUE)throw new IllegalArgumentException("modules/billford.yml reward weights are too large");}}
    private static void validateTools(ToolsFile config){if(config.tools().isEmpty())throw new IllegalArgumentException("modules/tools.yml tools must not be empty");Set<String>ids=new java.util.HashSet<>();for(var tool:config.tools()){if(tool.id()==null||!tool.id().matches("[a-z0-9_-]{1,32}")||!ids.add(tool.id())||tool.material()==null||!tool.material().matches("[A-Z0-9_]+")||tool.displayName()==null||tool.displayName().isBlank()||tool.durability()<1||tool.cooldownMillis()<0||!Set.of("VANILLA").contains(tool.dropPolicy())||tool.blockAllowlist().isEmpty()||tool.blockAllowlist().stream().anyMatch(value->!value.matches("[A-Z0-9_]+"))||tool.upgrades().isEmpty())throw new IllegalArgumentException("modules/tools.yml tool is invalid");Set<Integer>levels=new java.util.HashSet<>();for(var upgrade:tool.upgrades())if(upgrade.level()<1||!levels.add(upgrade.level())||!Set.of("PLANE","CUBE","TUNNEL").contains(upgrade.shape())||upgrade.radius()<0||upgrade.radius()>4||upgrade.depth()<1||upgrade.depth()>16||upgrade.dropMultiplier()<1||upgrade.dropMultiplier()>10)throw new IllegalArgumentException("modules/tools.yml upgrade is invalid");if(!levels.contains(1))throw new IllegalArgumentException("modules/tools.yml tools need level 1");if(tool.recipe().enabled()&&(tool.recipe().shape().size()!=3||tool.recipe().shape().stream().anyMatch(row->row.length()!=3)||tool.recipe().ingredients().entrySet().stream().anyMatch(entry->entry.getKey().length()!=1||!entry.getValue().matches("[A-Z0-9_]+"))))throw new IllegalArgumentException("modules/tools.yml recipe is invalid");}}

    private static void validateSecureStorage(SecureStorageFile config){if(config.leaseSeconds()<10||config.leaseSeconds()>3600||config.maximumVaults()<1||config.maximumVaults()>100
            ||config.vaultRowsLimit()==null||!config.vaultRowsLimit().matches("[A-Z][A-Z0-9_]*")||config.maximumItemPayloadBytes()<1
            ||config.maximumContainerPayloadBytes()<config.maximumItemPayloadBytes()||config.maximumContainerPayloadBytes()>16_777_216
            ||!Set.of("DENY_ALL","DENY_NON_EMPTY","ALLOW").contains(config.nestedContainerPolicy())||!Set.of("DENY","ALLOW").contains(config.customItemPolicy())
            ||config.adminCapability()==null||!config.adminCapability().matches("[A-Z][A-Z0-9_]*"))throw new IllegalArgumentException("modules/secure-storage.yml policy is invalid");}
    private static void validateDiscordBridge(DiscordBridgeFile config){if(config.secretEnv()==null||!config.secretEnv().matches("[A-Z][A-Z0-9_]*")||config.linkCodeTtlSeconds()<60||config.linkCodeTtlSeconds()>3600
            ||config.messageMaximumAgeSeconds()<5||config.messageMaximumAgeSeconds()>600||config.maximumMessagesPerMinute()<1||config.maximumMessagesPerMinute()>10_000
            ||config.maximumRetryAttempts()<1||config.maximumRetryAttempts()>100||config.retryBaseSeconds()<1||config.retryBaseSeconds()>3600
            ||config.bindHost()==null||config.bindHost().isBlank()||config.bindPort()<1||config.bindPort()>65535)throw new IllegalArgumentException("modules/discord-bridge.yml policy is invalid");}
    private static void validatePlayerWarps(PlayerWarpsFile config,EconomyFile economy){if(config.categories().isEmpty()||new java.util.HashSet<>(config.categories()).size()!=config.categories().size()||config.categories().stream().anyMatch(value->!value.matches("[A-Z][A-Z0-9_]*"))||config.defaultExpirySeconds()<0)throw new IllegalArgumentException("modules/playerwarps.yml categories/expiry are invalid");var sponsor=config.sponsorship();if(!economy.definitions().containsKey(sponsor.currency())||sponsor.pricePerHourMinor()<1||sponsor.minimumDurationSeconds()<60||sponsor.maximumDurationSeconds()<sponsor.minimumDurationSeconds()||sponsor.maximumActiveGlobal()<1||sponsor.maximumActivePerPlayer()<1||sponsor.maximumActivePerPlayer()>sponsor.maximumActiveGlobal())throw new IllegalArgumentException("modules/playerwarps.yml sponsorship policy is invalid");}
    private static void validateMenus(MenusFile config){List<String>materials=new java.util.ArrayList<>(List.of(config.theme().fillMaterial(),config.theme().accentMaterial(),config.theme().positiveMaterial(),config.theme().negativeMaterial(),config.theme().previousMaterial(),config.theme().closeMaterial(),config.theme().nextMaterial()));if(materials.stream().anyMatch(value->value==null||!value.matches("[A-Z0-9_]+")))throw new IllegalArgumentException("modules/menus.yml theme materials are invalid");if(config.layouts().isEmpty()||config.layouts().entrySet().stream().anyMatch(entry->!entry.getKey().matches("[a-z][a-z0-9_-]*")||entry.getValue().rows()<1||entry.getValue().rows()>6||entry.getValue().title()==null||entry.getValue().title().isBlank()))throw new IllegalArgumentException("modules/menus.yml layouts are invalid");Set<Integer>slots=new java.util.HashSet<>();for(var entry:config.rootEntries()){if(entry.id()==null||!entry.id().matches("[A-Z][A-Z0-9_]*")||entry.slot()<0||entry.slot()>=54||!slots.add(entry.slot())||entry.material()==null||!entry.material().matches("[A-Z0-9_]+")||entry.name()==null||entry.name().isBlank()||!config.layouts().containsKey(entry.menuId())||(entry.requiredCapability()!=null&&!entry.requiredCapability().isBlank()&&!entry.requiredCapability().matches("[A-Z][A-Z0-9_]*")))throw new IllegalArgumentException("modules/menus.yml root entry is invalid");}}
    private static void validateMenuText(MenusFile config){MiniMessageRenderer renderer=new MiniMessageRenderer();config.layouts().forEach((id,layout)->renderer.validate(com.magicstudios.magiccore.gui.GuiMarkup.complete(layout.title())));for(var entry:config.rootEntries()){renderer.validate(com.magicstudios.magiccore.gui.GuiMarkup.complete(entry.name()));entry.lore().forEach(line->renderer.validate(com.magicstudios.magiccore.gui.GuiMarkup.complete(line)));}}

    private static void validateEvents(EventsFile config,EconomyFile economy){var koth=config.koth();if(koth==null||koth.tickSeconds()<1||koth.tickSeconds()>60)throw new IllegalArgumentException("modules/events.yml KOTH tick policy is invalid");Set<String>ids=new java.util.HashSet<>();for(var definition:koth.definitions()){if(definition.id()==null||!definition.id().matches("[A-Z][A-Z0-9_]*")||!ids.add(definition.id())||definition.displayName()==null||definition.displayName().isBlank()||definition.world()==null||definition.world().isBlank()||definition.minimumX()>definition.maximumX()||definition.minimumY()>definition.maximumY()||definition.minimumZ()>definition.maximumZ()||definition.captureSeconds()<1||definition.firstDelaySeconds()<0||definition.scheduleIntervalSeconds()<1||definition.bannedMaterials().stream().anyMatch(value->value==null||!value.matches("[A-Z0-9_]+")))throw new IllegalArgumentException("modules/events.yml contains an invalid KOTH definition");validateEventReward(definition.reward(),economy,"KOTH");}var party=config.voteParty();if(party==null||party.threshold()<1||!Set.of("COUNT_AND_REWARD","COUNT_ONLY","IGNORE").contains(party.offlinePolicy()))throw new IllegalArgumentException("modules/events.yml vote-party policy is invalid");var pinata=party.pinata();if(pinata==null||pinata.world()==null||pinata.world().isBlank()||pinata.entityType()==null||!pinata.entityType().matches("[A-Z0-9_]+")||pinata.maximumHits()<1||pinata.maximumHitsPerPlayer()<1||pinata.maximumHitsPerPlayer()>pinata.maximumHits()||pinata.bossbarTitle()==null||pinata.bossbarTitle().isBlank())throw new IllegalArgumentException("modules/events.yml pinata policy is invalid");validateEventReward(pinata.hitReward(),economy,"pinata hit");validateEventReward(pinata.finalReward(),economy,"pinata final");new MiniMessageRenderer().validate(pinata.bossbarTitle());Set<String>announcementIds=new java.util.HashSet<>();for(var announcement:config.announcements()){if(announcement.id()==null||!announcement.id().matches("[A-Z][A-Z0-9_]*")||!announcementIds.add(announcement.id())||announcement.firstDelaySeconds()<0||announcement.intervalSeconds()<1||announcement.message()==null||announcement.message().isBlank()||announcement.sound()==null||(!announcement.sound().isBlank()&&!announcement.sound().matches("[A-Z0-9_]+")))throw new IllegalArgumentException("modules/events.yml contains an invalid announcement");var renderer=new MiniMessageRenderer();renderer.validate(announcement.message());if(announcement.title()!=null&&!announcement.title().isBlank())renderer.validate(announcement.title());if(announcement.subtitle()!=null&&!announcement.subtitle().isBlank())renderer.validate(announcement.subtitle());}if(config.maintenance()==null||config.maintenance().sponsorshipExpiryIntervalSeconds()<10||config.maintenance().secureStorageRecoveryIntervalSeconds()<10)throw new IllegalArgumentException("modules/events.yml maintenance intervals are invalid");}
    private static void validateEventReward(EventsFile.EventReward reward,EconomyFile economy,String path){if(reward==null||!economy.definitions().containsKey(reward.currency())||reward.amountMinor()<0)throw new IllegalArgumentException("modules/events.yml "+path+" reward is invalid");}

    private static void validateNavigation(String section, List<PresentationFile.NavigationEntry> entries,
                                           Set<String> ids, Set<Integer> slots) {
        if (entries.isEmpty()) throw new IllegalArgumentException("modules/presentation.yml " + section + " must not be empty");
        for (var entry : entries) {
            if (entry.id() == null || !entry.id().matches("[a-z0-9_-]{1,32}") || !ids.add(entry.id()))
                throw new IllegalArgumentException("modules/presentation.yml has an invalid/duplicate " + section + " id");
            if (entry.title() == null || entry.title().isBlank() || entry.description() == null || entry.description().isBlank()
                    || entry.action() == null || entry.action().isBlank())
                throw new IllegalArgumentException("modules/presentation.yml " + section + " entries require title, description, and action");
            if (entry.material() == null || !entry.material().matches("[A-Z0-9_]+") || entry.slot() < 0 || entry.slot() > 53 || !slots.add(entry.slot()))
                throw new IllegalArgumentException("modules/presentation.yml " + section + " material/slot is invalid or duplicated");
            if (entry.requiredCapability() != null && !entry.requiredCapability().isBlank()
                    && !entry.requiredCapability().matches("[A-Z][A-Z0-9_]*"))
                throw new IllegalArgumentException("modules/presentation.yml requires canonical capability IDs");
        }
    }

    private static void validateApplication(String id, PresentationFile.ApplicationDefinition application) {
        if (application.title() == null || application.title().isBlank() || application.applyUrl() == null || application.applyUrl().isBlank()
                || application.requirements().isEmpty())
            throw new IllegalArgumentException("modules/presentation.yml application " + id + " is incomplete");
        try {
            java.net.URI uri = java.net.URI.create(application.applyUrl());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("modules/presentation.yml application " + id + " requires an HTTPS apply-url", failure);
        }
        Set<String> requirementIds = new java.util.HashSet<>();
        Set<String> types = Set.of("PLAYTIME_SECONDS", "KILLS", "DEATHS", "DEATHS_MAXIMUM", "ACCOUNT_AGE_SECONDS",
                "SHARDS", "DISCORD_LINKED", "SETTING_ENABLED", "CAPABILITY");
        for (var requirement : application.requirements()) {
            if (requirement.id() == null || !requirement.id().matches("[a-z0-9_-]{1,32}") || !requirementIds.add(requirement.id())
                    || requirement.label() == null || requirement.label().isBlank() || requirement.type() == null
                    || !types.contains(requirement.type().toUpperCase(java.util.Locale.ROOT)) || requirement.target() < 0)
                throw new IllegalArgumentException("modules/presentation.yml application " + id + " has an invalid requirement");
            String type = requirement.type().toUpperCase(java.util.Locale.ROOT);
            if ((type.equals("CAPABILITY") || type.equals("SETTING_ENABLED"))
                    && (requirement.capability() == null || requirement.capability().isBlank()))
                throw new IllegalArgumentException("modules/presentation.yml " + type + " requirements need capability");
            if (type.equals("CAPABILITY") && !requirement.capability().matches("[A-Z][A-Z0-9_]*"))
                throw new IllegalArgumentException("modules/presentation.yml requires canonical capability IDs");
            if (type.equals("SETTING_ENABLED")) {
                try { com.magicstudios.magiccore.modules.settings.PlayerSetting.valueOf(requirement.capability().toUpperCase(java.util.Locale.ROOT)); }
                catch (IllegalArgumentException failure) { throw new IllegalArgumentException("modules/presentation.yml contains an unknown setting requirement", failure); }
            }
        }
    }

    private static void validateAfk(AfkFile afk){var policy=afk.policy();var eligibility=afk.eligibility();if(policy.intervalSeconds()<1||policy.baseShards()<1||policy.dailyCap()<policy.baseShards()||policy.reconnectProtectionSeconds()<0||policy.diminishingAfter()<0||policy.diminishingBasisPoints()<1||policy.diminishingBasisPoints()>10_000)throw new IllegalArgumentException("modules/afk.yml policy is invalid");
        if(eligibility.minimumSessionSeconds()<0||eligibility.minimumPresenceSamples()<1||eligibility.minimumDistinctPositions()<1||eligibility.minimumLookChanges()<0||eligibility.maximumMacroRiskBasisPoints()<0||eligibility.maximumMacroRiskBasisPoints()>10_000)throw new IllegalArgumentException("modules/afk.yml eligibility is invalid");
        Set<String>ids=new java.util.HashSet<>();if(afk.zones().isEmpty())throw new IllegalArgumentException("modules/afk.yml requires at least one zone");for(var zone:afk.zones()){if(!ids.add(zone.id())||!zone.id().matches("[a-z][a-z0-9_-]{0,31}"))throw new IllegalArgumentException("Invalid AFK zone ID "+zone.id());String type=zone.type().toUpperCase(java.util.Locale.ROOT);if(!Set.of("NATIVE","WORLDGUARD").contains(type))throw new IllegalArgumentException("Unknown AFK zone type "+zone.type());if(type.equals("NATIVE")&&(zone.world().isBlank()||zone.minimumX()>zone.maximumX()||zone.minimumY()>zone.maximumY()||zone.minimumZ()>zone.maximumZ()))throw new IllegalArgumentException("Invalid native AFK zone "+zone.id());if(type.equals("WORLDGUARD")&&zone.worldGuardRegion().isBlank())throw new IllegalArgumentException("WorldGuard AFK zone requires a region ID");}}

    private static void validateIntegrationProviders(IntegrationsFile integrations){
        requireProvider("luckperms.mode",integrations.luckperms().mode(),Set.of("INTERNAL","HYBRID","LUCKPERMS"));
        requireProvider("claims.provider",integrations.claims().provider(),Set.of("AUTO","NONE","GRIEFPREVENTION"));
        requireProvider("discord.provider",integrations.discord().provider(),Set.of("DISCORDSRV","CUSTOM_BOT","NONE"));
        requireProvider("lunar-client.provider",integrations.lunarClient().provider(),Set.of("APOLLO","NONE"));
        requireProvider("display.provider",integrations.display().provider(),Set.of("INTERNAL","TAB"));
        requireProvider("custom-items.provider",integrations.customItems().provider(),Set.of("NONE","ITEMSADDER","NEXO"));
        requireProvider("holograms.provider",integrations.holograms().provider(),Set.of("NONE","DECENT_HOLOGRAMS"));
        requireProvider("npcs.provider",integrations.npcs().provider(),Set.of("NONE","CITIZENS"));
        requireProvider("spawners.provider",integrations.spawners().provider(),Set.of("AUTO","NONE","ROSESTACKER","WILDSTACKER"));
        requireProvider("crates.provider",integrations.crates().provider(),Set.of("INTERNAL","EXCELLENTCRATES"));
        if(integrations.vulcan().retentionSeconds()<1||integrations.vulcan().maximumFlagsPerPlayer()<1||integrations.vulcan().maximumFlagsPerPlayer()>10_000)throw new IllegalArgumentException("integrations.yml vulcan retention/flag bounds are invalid");
    }
    private static void requireProvider(String path,String value,Set<String>allowed){if(value==null||!allowed.contains(value.toUpperCase(java.util.Locale.ROOT)))throw new IllegalArgumentException("integrations.yml "+path+" is invalid: "+value);}

    private static Map<String, String> flattenMessages(Path file) throws Exception {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object loaded = new Yaml(new SafeConstructor(options)).load(Files.readString(file));
        if (!(loaded instanceof Map<?, ?> root)) throw new IllegalArgumentException("messages.yml root must be a map");
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", root, flattened);
        flattened.remove("config-version");
        return Map.copyOf(flattened);
    }

    private static void flatten(String prefix, Map<?, ?> values, Map<String, String> target) {
        values.forEach((key, value) -> {
            String path = prefix.isEmpty() ? key.toString() : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) flatten(path, nested, target);
            else if (value instanceof String string) target.put(path, string);
        });
    }
}
