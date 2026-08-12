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

import java.util.Map;

public record MagicCoreConfiguration(CoreFile core, FeaturesFile features, IntegrationsFile integrations,
                                     StorageFile storage, RanksFile ranks, EconomyFile economy,
                                     TeamsFile teams, RewardsFile rewards,
                                     EssentialsFile essentials, ShopFile shop, SettingsFile settings,
                                     AuctionFile auction,
                                     OrdersFile orders,
                                     BountiesFile bounties,
                                     LifestealFile lifesteal,
                                     CombatFile combat,
                                     CratesFile crates,
                                     DisplayFile display,
                                     StoreFile store,
                                     AfkFile afk,
                                     PresentationFile presentation,
                                     SpawnStashFile spawnStash,
                                     ItemWorthFile itemWorth,
                                     BillfordFile billford,
                                     ToolsFile tools,
                                     SecureStorageFile secureStorage,
                                     DiscordBridgeFile discordBridge,
                                     PlayerWarpsFile playerWarps,
                                     MenusFile menus,
                                     EventsFile events,
                                     Map<String, String> messages) {
    public MagicCoreConfiguration {
        messages = Map.copyOf(messages);
    }
}
