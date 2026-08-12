package com.magicstudios.magiccore.commands;

import com.magicstudios.magiccore.admin.SetupPreset;
import com.magicstudios.magiccore.admin.SetupService;
import com.magicstudios.magiccore.admin.AdminActor;
import com.magicstudios.magiccore.admin.AdminEditingService;
import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.bootstrap.MagicCoreRuntime;
import com.magicstudios.magiccore.bootstrap.FastCrystalController;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.BukkitItemFingerprint;
import com.magicstudios.magiccore.platform.BukkitWorldPositions;
import com.magicstudios.magiccore.modules.essentials.HomeService;
import com.magicstudios.magiccore.modules.essentials.WarpService;
import com.magicstudios.magiccore.modules.essentials.WarpAccess;
import com.magicstudios.magiccore.modules.essentials.BackService;
import com.magicstudios.magiccore.modules.essentials.TeleportService;
import com.magicstudios.magiccore.modules.essentials.TeleportRequestService;
import com.magicstudios.magiccore.modules.essentials.TeleportRequest;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.modules.essentials.RtpService;
import com.magicstudios.magiccore.modules.essentials.RtpBounds;
import com.magicstudios.magiccore.modules.kits.KitService;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.modules.shop.ShopService;
import com.magicstudios.magiccore.modules.shop.SellService;
import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpService;
import com.magicstudios.magiccore.modules.auction.AuctionService;
import com.magicstudios.magiccore.modules.orders.OrderService;
import com.magicstudios.magiccore.modules.bounties.BountyService;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceAnalyticsService;
import com.magicstudios.magiccore.admin.MarketplaceAdminService;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.combat.CombatService;
import com.magicstudios.magiccore.modules.combat.NewPlayerProtectionService;
import com.magicstudios.magiccore.bootstrap.PhaseTwoPlayerListener;
import com.magicstudios.magiccore.modules.crates.CrateService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.modules.statistics.CachedStatsLeaderboards;
import com.magicstudios.magiccore.modules.statistics.StatsMetric;
import com.magicstudios.magiccore.modules.store.StoreService;
import net.kyori.adventure.text.event.ClickEvent;
import com.magicstudios.magiccore.imports.ImportService;
import com.magicstudios.magiccore.imports.ImportTarget;
import com.magicstudios.magiccore.imports.ImportTargets;
import com.magicstudios.magiccore.imports.CsvImportSource;
import com.magicstudios.magiccore.modules.display.CompetitiveLeaderboardService;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.presentation.PresentationService;
import com.magicstudios.magiccore.modules.presentation.ApplicationKind;
import com.magicstudios.magiccore.modules.profiles.ProfileViewService;
import com.magicstudios.magiccore.bootstrap.SpawnStashController;
import com.magicstudios.magiccore.modules.spawnstash.SpawnStashCase;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.worth.ItemValuationService;
import com.magicstudios.magiccore.modules.resets.ResetAdminService;
import com.magicstudios.magiccore.modules.keyall.KeyallService;
import com.magicstudios.magiccore.bootstrap.KeyallController;
import com.magicstudios.magiccore.modules.gemshop.GemShopService;
import com.magicstudios.magiccore.modules.economy.Money;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.integrations.discord.DiscordIntegrationService;
import com.magicstudios.magiccore.integrations.discord.CustomDiscordBridge;
import com.magicstudios.magiccore.platform.BukkitValuationInputs;
import com.magicstudios.magiccore.modules.shop.AdvancedSellService;
import com.magicstudios.magiccore.modules.shop.SellScope;
import com.magicstudios.magiccore.modules.billford.*;
import com.magicstudios.magiccore.bootstrap.CustomToolController;
import com.magicstudios.magiccore.gui.MagicGuiController;
import com.magicstudios.magiccore.modules.events.KothService;
import com.magicstudios.magiccore.modules.events.VotePartyService;
import com.magicstudios.magiccore.bootstrap.PinataController;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

public final class MagicCoreCommand implements CommandExecutor, TabCompleter {
    private final AtomicReference<MagicCoreRuntime> runtime;
    private final SchedulerFacade scheduler;

    public MagicCoreCommand(AtomicReference<MagicCoreRuntime> runtime, SchedulerFacade scheduler) {
        this.runtime = runtime;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        MagicCoreRuntime active = runtime.get();
        if (active == null || !active.ready()) {
            sender.sendMessage(Component.text("MagicCore is still starting. Check /magic diagnose shortly."));
            return true;
        }
        if(command.getName().equalsIgnoreCase("spawnstash")){String[] forwarded=new String[args.length+1];forwarded[0]="spawnstash";System.arraycopy(args,0,forwarded,1,args.length);args=forwarded;}
        String subcommand = args.length == 0 ? "diagnose" : args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "diagnose" -> {
                sender.sendMessage(Component.text("MagicCore diagnostics:"));
                active.diagnostics().inspectAll().forEach((id, report) ->
                        sender.sendMessage(Component.text("- " + id + ": " + report.state() + " — " + report.reason())));
            }
            case "setup" -> setup(sender, active, args);
            case "admin" -> admin(sender, active, args);
            case "reload" -> active.reload().whenComplete((result, failure) -> sendLater(sender, failure == null
                    ? (result.restartRequired() ? "Validated changes require a restart: " + result.changedSections()
                    : "Safe configuration reload applied: " + result.changedSections())
                    : "Reload rejected; the last valid snapshot remains active: " + failure.getMessage()));
            case "home" -> home(sender, active, args);
            case "warp" -> warp(sender, active, args);
            case "tpa" -> tpa(sender, active, args);
            case "back" -> back(sender, active);
            case "kit" -> kit(sender, active, args);
            case "settings" -> settings(sender, active, args);
            case "shop" -> shop(sender, active, args);
            case "sell" -> sell(sender, active, args);
            case "pwarp" -> playerWarp(sender, active, args);
            case "rtp" -> rtp(sender, active);
            case "spawn" -> spawn(sender, active);
            case "setspawn" -> setSpawn(sender, active);
            case "auction" -> auction(sender, active, args);
            case "order" -> order(sender,active,args);
            case "bounty" -> bounty(sender,active,args);
            case "market" -> market(sender,active,args);
            case "hearts" -> hearts(sender,active,args);
            case "combat" -> combat(sender,active,args);
            case "crate" -> crate(sender,active,args);
            case "stats" -> stats(sender,active,args);
            case "store" -> store(sender,active,args);
            case "import" -> importData(sender,active,args);
            case "leaderboard" -> leaderboard(sender,active,args);
            case "shards","afk" -> shards(sender,active,args);
            case "info" -> presentation(sender,active,false);
            case "server" -> presentation(sender,active,true);
            case "apply" -> application(sender,active,args);
            case "profile" -> profile(sender,active,args);
            case "spawnstash" -> spawnStash(sender,active,args);
            case "worth" -> worth(sender,active);
            case "billford" -> billford(sender,active,args);
            case "tool" -> customTool(sender,active,args);
            case "reset" -> reset(sender,active,args);
            case "keyall" -> keyall(sender,active,args);
            case "gemshop" -> gemshop(sender,active,args);
            case "gems" -> gems(sender,active,args);
            case "discord" -> discord(sender,active,args);
            case "menu" -> {if(active.services().find(MagicGuiController.class).isEmpty())sendLater(sender,"Menus are disabled.");else menu(sender,active,args);}
            case "koth" -> {if(active.services().find(KothService.class).isEmpty())sendLater(sender,"KOTH is disabled.");else koth(sender,active,args);}
            case "vote" -> {if(active.services().find(VotePartyService.class).isEmpty())sendLater(sender,"Vote party is disabled.");else vote(sender,active,args);}
            default -> sender.sendMessage(Component.text("Usage: /magic <menu|koth|vote|setup|admin|diagnose|reload|info|server|apply|profile|home|warp|tpa|back|rtp|spawn|setspawn|kit|settings|shop|sell|pwarp|auction|order|bounty|market|hearts|combat|crate|stats|store|import|leaderboard|shards|reset>"));
        }
        return true;
    }

    private void home(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        HomeService homes = active.services().require(HomeService.class);
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (action.equals("list")) {
            homes.homes(player.getUniqueId()).whenComplete((values, failure) -> reply(sender, failure,
                    () -> "Homes: " + values.stream().map(value -> value.displayName()).toList()));
        } else if (action.equals("set") && args.length > 2) {
            scheduler.executeEntity(player, () -> homes.set(player.getUniqueId(), args[2],
                    BukkitWorldPositions.from(player.getLocation()), operation("home-set"))
                    .whenComplete((value, failure) -> reply(sender, failure, () -> "Home " + value.code().toLowerCase(Locale.ROOT))), () -> { });
        } else if (action.equals("delete") && args.length > 2) {
            homes.delete(player.getUniqueId(), args[2], operation("home-delete"))
                    .whenComplete((value, failure) -> reply(sender, failure, () -> "Home " + value.code().toLowerCase(Locale.ROOT)));
        } else if (args.length > 1) {
            homes.findVisible(player.getUniqueId(), player.getUniqueId(), args[1]).whenComplete((home, failure) -> {
                if (failure != null) { reply(sender, failure, () -> ""); return; }
                if (home.isEmpty()) { sendLater(sender, "Home not found."); return; }
                teleport(active, player, home.get().position());
            });
        } else sendLater(sender, "Usage: /magic home <list|set name|delete name|name>");
    }

    private void warp(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        WarpService warps = active.services().require(WarpService.class);
        if (args.length < 2 || args[1].equalsIgnoreCase("list"))
            warps.visibleWarps(player.getUniqueId()).whenComplete((values, failure) -> reply(sender, failure,
                    () -> "Warps: " + values.stream().map(value -> value.displayName()).toList()));
        else warps.findVisible(player.getUniqueId(), args[1]).whenComplete((warp, failure) -> {
            if (failure != null) { reply(sender, failure, () -> ""); return; }
            if (warp.isEmpty()) sendLater(sender, "Warp not found or unavailable."); else teleport(active, player, warp.get().position());
        });
    }

    private void tpa(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        TeleportRequestService requests = active.services().require(TeleportRequestService.class);
        if (args.length > 1 && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
            if (args[1].equalsIgnoreCase("deny")) { sendLater(sender, requests.deny(player.getUniqueId()) ? "Teleport request denied." : "No request found."); return; }
            var accepted = requests.accept(player.getUniqueId());
            if (accepted.isEmpty()) { sendLater(sender, "No active teleport request."); return; }
            TeleportRequest request = accepted.get();
            scheduler.executeGlobal(() -> {
                Player requester = player.getServer().getPlayer(request.requesterId());
                if (requester == null) { sendLater(sender, "Requester is offline."); return; }
                Player moving = request.direction() == TeleportRequest.Direction.REQUESTER_TO_TARGET ? requester : player;
                Player target = request.direction() == TeleportRequest.Direction.REQUESTER_TO_TARGET ? player : requester;
                scheduler.executeEntity(target, () -> active.services().require(TeleportService.class)
                        .teleport(moving, target.getLocation(), teleportWarmup(active), operation("tpa"))
                        .whenComplete((value, failure) -> reply(sender, failure, value::code)), () -> { });
            });
        } else if (args.length > 1 && args[1].equalsIgnoreCase("cancel"))
            sendLater(sender, requests.cancel(player.getUniqueId()) ? "Teleport request cancelled." : "No request found.");
        else if (args.length > 1) scheduler.executeGlobal(() -> {
            boolean here = args[1].equalsIgnoreCase("here") && args.length > 2;
            String targetName = here ? args[2] : args[1];
            Player target = player.getServer().getPlayerExact(targetName);
            if (target == null) { sendLater(sender, "Player not found."); return; }
            requests.request(player.getUniqueId(), target.getUniqueId(), here
                            ? TeleportRequest.Direction.TARGET_TO_REQUESTER : TeleportRequest.Direction.REQUESTER_TO_TARGET)
                    .whenComplete((value, failure) -> reply(sender, failure, () -> "Teleport request sent to " + target.getName()));
        }); else sendLater(sender, "Usage: /magic tpa <player|here player|accept|deny|cancel>");
    }

    private void back(CommandSender sender, MagicCoreRuntime active) {
        Player player = requirePlayer(sender); if (player == null) return;
        active.services().require(BackService.class).previousTeleport(player.getUniqueId()).whenComplete((position, failure) -> {
            if (failure != null) { reply(sender, failure, () -> ""); return; }
            if (position.isEmpty()) sendLater(sender, "No previous location."); else teleport(active, player, position.get());
        });
    }

    private void kit(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        KitService kits = active.services().require(KitService.class);
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) sendLater(sender, "Kits: " + kits.definitions().stream().map(k -> k.id()).toList());
        else kits.claim(player.getUniqueId(), args[1], operation("kit")).whenComplete((value, failure) -> reply(sender, failure, value::code));
    }

    private void settings(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        if (args.length < 3) { sendLater(sender, "Usage: /magic settings <name> <true|false>"); return; }
        try {
            PlayerSetting setting = PlayerSetting.valueOf(args[1].toUpperCase(Locale.ROOT));
            boolean enabled = Boolean.parseBoolean(args[2]);
            active.services().require(PlayerSettingsService.class).set(player.getUniqueId(), setting, enabled, operation("setting"))
                    .whenComplete((value, failure) -> {
                        if (failure == null && setting == PlayerSetting.FAST_CRYSTAL)
                            active.services().find(FastCrystalController.class).ifPresent(controller -> controller.refresh(player));
                        reply(sender, failure, () -> setting + " = " + value.enabled(setting));
                    });
        } catch (IllegalArgumentException failure) { sendLater(sender, "Unknown setting or value."); }
    }

    private void shop(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        ShopService shop = active.services().require(ShopService.class);
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) sendLater(sender, "Products: " + shop.products().stream().map(p -> p.id()).toList());
        else {
            int quantity = args.length > 2 ? Integer.parseInt(args[2]) : 1;
            shop.buy(player.getUniqueId(), args[1], quantity, operation("shop-buy"))
                    .whenComplete((value, failure) -> reply(sender, failure, value::code));
        }
    }

    private void sell(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        if(args.length>=2&&args[1].equalsIgnoreCase("history")){active.services().require(AdvancedSellService.class).history(player.getUniqueId(),10).whenComplete((values,failure)->reply(sender,failure,()->"Sell receipts: "+values.stream().map(value->value.id()+"="+value.creditedMinor()+" "+value.currency()).toList()));return;}
        if(args.length>=3&&args[1].equalsIgnoreCase("receipt")){try{UUID id=UUID.fromString(args[2]);active.services().require(AdvancedSellService.class).receipt(id).whenComplete((value,failure)->reply(sender,failure,()->value.map(Object::toString).orElse("Receipt not found.")));}catch(IllegalArgumentException invalid){sendLater(sender,"Invalid receipt UUID.");}return;}
        if(args.length>=4&&args[1].equalsIgnoreCase("reconcile")){UUID quoteId;try{quoteId=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sendLater(sender,"Invalid quote UUID.");return;}boolean confirmed=args[3].equalsIgnoreCase("confirmed");active.services().require(CapabilityService.class).has(player.getUniqueId(),"SELL_RECONCILE").whenComplete((allowed,failure)->{if(failure!=null){reply(sender,failure,()->"");return;}if(!allowed){sendLater(sender,"You do not have the sell reconciliation capability.");return;}active.services().require(AdvancedSellService.class).reconcile(quoteId,confirmed,operation("sell-reconcile")).whenComplete((receipt,reconcileFailure)->reply(sender,reconcileFailure,()->"Reconciled to receipt "+receipt.id()));});return;}
        if(args.length>=2&&Set.of("hand","all","category").contains(args[1].toLowerCase(Locale.ROOT))){SellScope scope=args[1].equalsIgnoreCase("hand")?SellScope.HAND:args[1].equalsIgnoreCase("all")?SellScope.ALL:SellScope.CATEGORY;String category=scope==SellScope.CATEGORY?(args.length>2?args[2]:""):"";if(scope==SellScope.CATEGORY&&category.isBlank()){sendLater(sender,"Usage: /magic sell category <category>");return;}
            scheduler.executeEntity(player,()->{java.util.List<org.bukkit.inventory.ItemStack>items=scope==SellScope.HAND?List.of(player.getInventory().getItemInMainHand()):java.util.Arrays.stream(player.getInventory().getStorageContents()).filter(item->item!=null&&!item.getType().isAir()).toList();var snapshot=items.stream().filter(item->item!=null&&!item.getType().isAir()).map(BukkitValuationInputs::from).toList();AdvancedSellService selling=active.services().require(AdvancedSellService.class);selling.quote(player.getUniqueId(),scope,category,snapshot,operation("advanced-sell-quote")).thenCompose(quote->selling.execute(player.getUniqueId(),quote.id(),operation("advanced-sell-execute"))).whenComplete((receipt,failure)->reply(sender,failure,()->"SOLD: receipt="+receipt.id()+", items="+receipt.lines().stream().mapToInt(line->line.quantity()).sum()+", credited="+receipt.creditedMinor()+" "+receipt.currency()));},()->{});return;}
        if (args.length < 3) { sendLater(sender, "Usage: /magic sell <hand|all|category name|history|receipt id|reconcile quote confirmed|not-removed> or /magic sell <product-id> <item-quantity>"); return; }
        int quantity = Integer.parseInt(args[2]);
        scheduler.executeEntity(player, () -> {
            var held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) { sendLater(sender, "Hold the exact item to sell."); return; }
            SellService selling = active.services().require(SellService.class);
            String quoteKey = operation("sell-quote");
            selling.quote(player.getUniqueId(), args[1], BukkitItemFingerprint.fingerprint(held), quantity, quoteKey)
                    .thenCompose(quote -> selling.execute(player.getUniqueId(), quote.id(), operation("sell-execute")))
                    .whenComplete((value, failure) -> reply(sender, failure, value::code));
        }, () -> { });
    }

    private void playerWarp(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        PlayerWarpService warps = active.services().require(PlayerWarpService.class);
        if (args.length < 2 || args[1].equalsIgnoreCase("list")||args[1].equalsIgnoreCase("search")){String text=args.length>2?args[2]:"";warps.search(new com.magicstudios.magiccore.modules.playerwarps.PlayerWarpQuery(text,"",null,player.getUniqueId(),com.magicstudios.magiccore.modules.playerwarps.PlayerWarpQuery.Sort.SPONSORED,0,50)).whenComplete((values,failure)->reply(sender,failure,()->"Player warps: "+values.stream().map(value->(value.promoted()?"[PROMOTED] ":"")+value.warp().displayName()+" visits="+value.warp().visits()+(value.favorite()?" ★":"")).toList()));}
        else if (args[1].equalsIgnoreCase("create") && args.length > 3)
            scheduler.executeEntity(player, () -> warps.create(player.getUniqueId(), args[2], args[3],
                    BukkitWorldPositions.from(player.getLocation()), operation("pwarp-create"))
                    .whenComplete((value, failure) -> reply(sender, failure, value::code)), () -> { });
        else if (args[1].equalsIgnoreCase("delete") && args.length > 2)
            warps.delete(player.getUniqueId(), args[2], operation("pwarp-delete"))
                    .whenComplete((value, failure) -> reply(sender, failure, value::code));
        else if(args[1].equalsIgnoreCase("favorite")&&args.length>3){boolean favorite=Boolean.parseBoolean(args[3]);warps.favorite(player.getUniqueId(),args[2],favorite,operation("pwarp-favorite")).whenComplete((changed,failure)->reply(player,failure,()->changed?"Favorite updated.":"Favorite was already in that state."));}
        else if(args[1].equalsIgnoreCase("sponsor")&&args.length>3){long seconds;try{seconds=Long.parseLong(args[3]);}catch(NumberFormatException invalid){sendLater(player,"Sponsorship duration must be seconds.");return;}warps.sponsor(player.getUniqueId(),args[2],Duration.ofSeconds(seconds),operation("pwarp-sponsor")).whenComplete((value,failure)->reply(player,failure,()->"Sponsorship "+value.id()+" active until "+value.endsAt()+"; charged="+value.chargedMinor()));}
        else if(args[1].equalsIgnoreCase("unsponsor")&&args.length>2){UUID id;try{id=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sendLater(player,"Invalid sponsorship UUID.");return;}warps.cancelSponsorship(player.getUniqueId(),id,operation("pwarp-unsponsor")).whenComplete((value,failure)->reply(player,failure,()->"Sponsorship cancelled; refund="+value.refundedMinor()));}
        else if(args[1].equalsIgnoreCase("transfer")&&args.length>3){UUID target=targetId(player,args[3]);if(target==null)return;warps.transfer(player.getUniqueId(),args[2],target,operation("pwarp-transfer")).whenComplete((value,failure)->reply(player,failure,value::code));}
        else if(args[1].equalsIgnoreCase("moderate")&&args.length>4){com.magicstudios.magiccore.modules.playerwarps.PlayerWarp.Status status;try{status=com.magicstudios.magiccore.modules.playerwarps.PlayerWarp.Status.valueOf(args[3].toUpperCase(Locale.ROOT));}catch(IllegalArgumentException invalid){sendLater(player,"Status must be ACTIVE, PENDING_REVIEW, or SUSPENDED.");return;}warps.moderate(player.getUniqueId(),args[2],status,String.join(" ",java.util.Arrays.copyOfRange(args,4,args.length)),operation("pwarp-moderate")).whenComplete((value,failure)->reply(player,failure,value::code));}
        else warps.prepareVisit(args[1],player.getUniqueId(),operation("pwarp-visit")).whenComplete((warp,failure)->{if(failure!=null){reply(sender,failure,()->"");return;}teleport(active,player,warp.position());});
    }

    private void rtp(CommandSender sender, MagicCoreRuntime active) {
        Player player = requirePlayer(sender); if (player == null) return;
        scheduler.executeEntity(player, () -> active.services().require(RtpService.class)
                .randomTeleport(player, player.getWorld(), active.services().require(RtpBounds.class), operation("rtp"))
                .whenComplete((value, failure) -> reply(sender, failure,
                        () -> value.code() + " after " + value.attempts() + " attempt(s)")), () -> { });
    }

    private void spawn(CommandSender sender, MagicCoreRuntime active) {
        Player player = requirePlayer(sender); if (player == null) return;
        active.services().require(WarpService.class).findVisible(player.getUniqueId(), "spawn").whenComplete((warp, failure) -> {
            if (failure != null) { reply(sender, failure, () -> ""); return; }
            if (warp.isEmpty()) sendLater(sender, "Spawn has not been configured."); else teleport(active, player, warp.get().position());
        });
    }

    private void setSpawn(CommandSender sender, MagicCoreRuntime active) {
        Player player = requirePlayer(sender); if (player == null) return;
        AdminActor actor = new AdminActor(player.getUniqueId(), player.getName(), false);
        scheduler.executeEntity(player, () -> active.services().require(WarpService.class).set(actor, "spawn",
                BukkitWorldPositions.from(player.getLocation()), WarpAccess.publicWarp(), operation("setspawn"))
                .whenComplete((value, failure) -> reply(sender, failure, value::code)), () -> { });
    }

    private void auction(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player = requirePlayer(sender); if (player == null) return;
        AuctionService auctions = active.services().require(AuctionService.class);
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        try {
            switch (action) {
                case "list" -> {
                    String query = args.length > 2 ? args[2] : "";
                    auctions.search(query, null, AuctionService.Sort.NEWEST, 0, 20)
                            .whenComplete((page, failure) -> reply(sender, failure, () -> "Auctions: " + page.listings().stream()
                                    .map(listing -> listing.id() + " " + listing.fingerprint().material() + " @ " + listing.priceMinor()).toList()));
                }
                case "sell" -> {
                    if (args.length < 6) { sendLater(sender, "Usage: /magic auction sell <category> <price-minor> <duration-seconds> <quantity>"); return; }
                    long price = Long.parseLong(args[3]); long seconds = Long.parseLong(args[4]); int quantity = Integer.parseInt(args[5]);
                    scheduler.executeEntity(player, () -> {
                        var held = player.getInventory().getItemInMainHand();
                        if (held.getType().isAir()) { sendLater(sender, "Hold the exact item to list."); return; }
                        auctions.create(player.getUniqueId(), args[2], BukkitItemFingerprint.fingerprint(held), quantity,
                                price, Duration.ofSeconds(seconds), operation("auction-create"))
                                .whenComplete((value, failure) -> reply(sender, failure,
                                        () -> value.code() + ": " + (value.listing() == null ? "" : value.listing().id())));
                    }, () -> { });
                }
                case "buy" -> {
                    if (args.length < 3) { sendLater(sender, "Usage: /magic auction buy <listing-id>"); return; }
                    auctions.purchase(player.getUniqueId(), UUID.fromString(args[2]), operation("auction-buy"))
                            .whenComplete((value, failure) -> reply(sender, failure, value::code));
                }
                case "cancel" -> {
                    if (args.length < 3) { sendLater(sender, "Usage: /magic auction cancel <listing-id>"); return; }
                    auctions.cancel(player.getUniqueId(), UUID.fromString(args[2]), operation("auction-cancel"))
                            .whenComplete((value, failure) -> reply(sender, failure, value::code));
                }
                case "history" -> auctions.history(player.getUniqueId(), 20).whenComplete((values, failure) ->
                        reply(sender, failure, () -> "Auction history: " + values.stream()
                                .map(value -> value.id() + "=" + value.status()).toList()));
                default -> sendLater(sender, "Usage: /magic auction <list|sell|buy|cancel|history>");
            }
        } catch (IllegalArgumentException failure) { sendLater(sender, "Invalid auction argument: " + failure.getMessage()); }
    }

    private void order(CommandSender sender,MagicCoreRuntime active,String[] args){Player player=requirePlayer(sender);if(player==null)return;
        OrderService orders=active.services().require(OrderService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        try{switch(action){
            case"list"->orders.open(args.length>2?args[2]:null,20).whenComplete((values,failure)->reply(sender,failure,
                    ()->"Orders: "+values.stream().map(o->o.id()+" "+o.fingerprint().material()+" "+o.availableQuantity()+" @ "+o.unitPriceMinor()).toList()));
            case"create"->{if(args.length<6){sendLater(sender,"Usage: /magic order create <category> <unit-price-minor> <quantity> <duration-seconds>");return;}
                long price=Long.parseLong(args[3]);int quantity=Integer.parseInt(args[4]);long seconds=Long.parseLong(args[5]);scheduler.executeEntity(player,()->{
                    var held=player.getInventory().getItemInMainHand();if(held.getType().isAir()){sendLater(sender,"Hold one sample of the requested item.");return;}
                    orders.create(player.getUniqueId(),args[2],BukkitItemFingerprint.fingerprint(held),quantity,price,Duration.ofSeconds(seconds),operation("order-create"))
                            .whenComplete((value,failure)->reply(sender,failure,()->value.code()+": "+(value.order()==null?"":value.order().id())));},()->{});}
            case"fill"->{if(args.length<4){sendLater(sender,"Usage: /magic order fill <order-id> <quantity>");return;}
                orders.fulfill(player.getUniqueId(),UUID.fromString(args[2]),Integer.parseInt(args[3]),operation("order-fill"))
                        .whenComplete((value,failure)->reply(sender,failure,value::code));}
            case"cancel"->{if(args.length<3){sendLater(sender,"Usage: /magic order cancel <order-id>");return;}
                orders.cancel(player.getUniqueId(),UUID.fromString(args[2]),operation("order-cancel"))
                        .whenComplete((value,failure)->reply(sender,failure,value::code));}
            case"history"->orders.history(player.getUniqueId(),20).whenComplete((values,failure)->reply(sender,failure,
                    ()->"Order history: "+values.stream().map(o->o.id()+"="+o.status()).toList()));
            default->sendLater(sender,"Usage: /magic order <list|create|fill|cancel|history>");}
        }catch(IllegalArgumentException failure){sendLater(sender,"Invalid order argument: "+failure.getMessage());}}

    private void bounty(CommandSender sender,MagicCoreRuntime active,String[] args){Player player=requirePlayer(sender);if(player==null)return;
        BountyService bounties=active.services().require(BountyService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        if(action.equals("list")){String query=args.length>2?args[2]:"";bounties.search(query,BountyService.Sort.VALUE_DESC,0,20).whenComplete((page,failure)->reply(sender,failure,
                ()->"Bounties: "+page.bounties().stream().map(b->b.targetId()+"="+b.totalEscrowMinor()).toList()));}
        else if(action.equals("history"))bounties.claimHistory(player.getUniqueId(),20).whenComplete((values,failure)->reply(sender,failure,
                ()->"Bounty history: "+values.stream().map(c->c.victimId()+"="+c.payoutMinor()).toList()));
        else if(action.equals("create")&&args.length>4&&args[4].equalsIgnoreCase("confirm")){long amount;try{amount=Long.parseLong(args[3]);}catch(NumberFormatException failure){sendLater(sender,"Invalid bounty amount.");return;}
            scheduler.executeGlobal(()->{Player target=player.getServer().getPlayerExact(args[2]);if(target==null){sendLater(sender,"Target must be online for unambiguous UUID selection.");return;}
                bounties.create(player.getUniqueId(),target.getUniqueId(),amount,operation("bounty-create"))
                        .whenComplete((value,failure)->reply(sender,failure,value::code));});}
        else sendLater(sender,"Usage: /magic bounty <list [query]|create player amount-minor confirm|history>");}

    private void market(CommandSender sender,MagicCoreRuntime active,String[]args){String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"stats";
        AdminActor actor=sender instanceof Player player?new AdminActor(player.getUniqueId(),player.getName(),false):AdminActor.consoleActor();
        if(action.equals("stats"))active.services().require(MarketplaceAdminService.class).snapshot(actor).whenComplete((snapshot,failure)->reply(sender,failure,
                ()->"Market: auctions="+snapshot.activeAuctions()+" orders="+snapshot.openOrders()+" bounties="+snapshot.activeBounties()+" volume="+snapshot.soldAuctionVolumeMinor()));
        else if(action.equals("expire"))active.services().require(MarketplaceAdminService.class).expireDue(actor,operation("market-expire"),100)
                .whenComplete((run,failure)->reply(sender,failure,()->"Expired auctions="+run.auctions()+", orders="+run.orders()));
        else if(action.equals("top")){String currency=args.length>2?args[2]:active.configuration().economy().primaryCurrency();
            active.services().require(MarketplaceAnalyticsService.class).balanceLeaderboard(currency,10).whenComplete((values,failure)->reply(sender,failure,
                    ()->"Top balances: "+values.stream().map(v->v.position()+". "+v.playerId()+"="+v.balanceMinor()).toList()));}
        else sendLater(sender,"Usage: /magic market <stats|expire|top [currency]>");}

    private void hearts(CommandSender sender,MagicCoreRuntime active,String[]args){LifestealService hearts=active.services().require(LifestealService.class);
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";
        if(action.equals("top")){hearts.leaderboard(10).whenComplete((values,failure)->reply(sender,failure,
                ()->"Top hearts: "+values.stream().map(value->value.playerId()+"="+value.hearts()).toList()));return;}
        Player player=requirePlayer(sender);if(player==null)return;
        if(action.equals("status")){hearts.account(player.getUniqueId()).whenComplete((account,failure)->reply(sender,failure,
                ()->"Hearts: "+account.hearts()+"; eliminated="+account.eliminated()));return;}
        if(action.equals("withdraw")){hearts.withdraw(player.getUniqueId(),operation("heart-withdraw")).whenComplete((value,failure)->{
            if(failure==null&&value.applied())active.services().find(PhaseTwoPlayerListener.class).ifPresent(listener->listener.deliverPending(player));
            reply(sender,failure,()->value.code()+"; hearts="+value.player().hearts());});return;}
        if(action.equals("revive")&&args.length>2){if(!sender.hasPermission("magiccore.admin.revive")){sendLater(sender,"You do not have permission to revive players.");return;}
            if(!active.configuration().lifesteal().revivalEnabled()){sendLater(sender,"Revival is disabled in modules/lifesteal.yml.");return;}
            scheduler.executeGlobal(()->{Player target=player.getServer().getPlayerExact(args[2]);if(target==null){sendLater(sender,"Target must be online for unambiguous UUID selection.");return;}
                hearts.revive(target.getUniqueId(),operation("heart-revive")).whenComplete((value,failure)->reply(sender,failure,
                        ()->value.code()+" "+target.getName()+" with "+value.player().hearts()+" hearts"));});return;}
        sendLater(sender,"Usage: /magic hearts <status|top|withdraw|revive player>");}

    private void combat(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;
        CombatService combat=active.services().require(CombatService.class);NewPlayerProtectionService newbies=active.services().require(NewPlayerProtectionService.class);
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";
        if(action.equals("status")){sendLater(sender,"Combat tagged="+combat.isTagged(player.getUniqueId())+", remaining="+combat.remaining(player.getUniqueId()).toSeconds()
                +"s, newbie-protection="+newbies.state(player.getUniqueId()));return;}
        if(action.equals("protection")&&args.length>2&&args[2].equalsIgnoreCase("remove")){newbies.remove(player.getUniqueId(),operation("newbie-remove"))
                .whenComplete((state,failure)->reply(sender,failure,()->"New-player protection: "+state));return;}
        sendLater(sender,"Usage: /magic combat <status|protection remove>");}

    private void crate(CommandSender sender,MagicCoreRuntime active,String[]args){CrateService crates=active.services().require(CrateService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        if(action.equals("list")){sendLater(sender,"Crates: "+crates.definitions().keySet());return;}
        if(action.equals("preview")&&args.length>2){var crate=crates.definitions().get(args[2].toUpperCase(Locale.ROOT));if(crate==null){sendLater(sender,"Unknown crate.");return;}
            sendLater(sender,"Rewards: "+crate.rewards().stream().map(reward->reward.id()+" ["+reward.rarity()+", weight="+reward.weight()+"]").toList());return;}
        Player player=requirePlayer(sender);if(player==null)return;
        if(action.equals("keys")&&args.length>2){crates.keyBalance(player.getUniqueId(),args[2].toUpperCase(Locale.ROOT)).whenComplete((balance,failure)->reply(sender,failure,()->balance.keyId()+" keys: "+balance.amount()));return;}
        if(action.equals("open")&&args.length>2){int amount=args.length>3?Integer.parseInt(args[3]):1;crates.open(player.getUniqueId(),args[2].toUpperCase(Locale.ROOT),amount,operation("crate-open"))
                .whenComplete((result,failure)->{if(failure==null&&result.applied())active.services().find(PhaseTwoPlayerListener.class).ifPresent(listener->listener.deliverPending(player));
                    reply(sender,failure,()->result.code()+": "+result.opening().rewards().stream().map(reward->reward.rewardId()).toList());});return;}
        if(action.equals("history")){crates.history(player.getUniqueId(),10).whenComplete((history,failure)->reply(sender,failure,
                ()->"Crate history: "+history.stream().map(opening->opening.crateId()+" x"+opening.amount()).toList()));return;}
        if(action.equals("grant")&&args.length>4){if(!sender.hasPermission("magiccore.admin.crates")){sendLater(sender,"You do not have permission to grant crate keys.");return;}
            long amount=Long.parseLong(args[4]);scheduler.executeGlobal(()->{Player target=player.getServer().getPlayerExact(args[2]);if(target==null){sendLater(sender,"Target must be online for unambiguous UUID selection.");return;}
                crates.grantKeys(target.getUniqueId(),args[3].toUpperCase(Locale.ROOT),amount,operation("crate-grant")).whenComplete((balance,failure)->reply(sender,failure,()->"Granted; balance="+balance.amount()));});return;}
        sendLater(sender,"Usage: /magic crate <list|preview id|keys key-id|open id [amount]|history|grant player key-id amount>");}

    private void stats(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;
        if(args.length>1&&args[1].equalsIgnoreCase("top")){StatsMetric metric=args.length>2?StatsMetric.valueOf(args[2].toUpperCase(Locale.ROOT)):StatsMetric.KILLS;
            active.services().require(CachedStatsLeaderboards.class).get(metric,10).whenComplete((entries,failure)->reply(sender,failure,()->metric+": "+entries.stream().map(entry->entry.position()+". "+entry.playerId()+"="+entry.value()).toList()));return;}
        active.services().require(PlayerStatsService.class).stats(player.getUniqueId()).whenComplete((value,failure)->reply(sender,failure,
                ()->"Stats: kills="+value.kills()+", deaths="+value.deaths()+", playtime="+value.playtimeSeconds()+"s"));}

    private void store(CommandSender sender,MagicCoreRuntime active,String[]args){StoreService store=active.services().require(StoreService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"link";
        if(action.equals("products")){sendLater(sender,"Products: "+store.products().values().stream().map(product->product.id()+" - "+product.displayName()).toList());return;}
        if(action.equals("goal")){store.donationGoal().whenComplete((goal,failure)->reply(sender,failure,()->"Donation goal: "+goal.contributedMinor()+"/"+goal.targetMinor()+" ("+Math.round(goal.progress()*100)+"%)"));return;}
        sender.sendMessage(Component.text("Open the MagicCore store: "+store.url()).clickEvent(ClickEvent.openUrl(store.url())));}

    private void importData(CommandSender sender,MagicCoreRuntime active,String[]args){if(!sender.hasPermission("magiccore.admin.import")){sendLater(sender,"You do not have permission to run imports.");return;}
        if(args.length>2&&args[1].equalsIgnoreCase("status")){active.services().require(ImportService.class).job(args[2]).whenComplete((job,failure)->reply(sender,failure,()->job.map(Object::toString).orElse("Import not found.")));return;}
        if(args.length<5){sendLater(sender,"Usage: /magic import <preview|execute|reconcile> <id> <file.csv> <profiles|balances|ranks|crate-keys> [field=column ...]");return;}
        String action=args[1].toLowerCase(Locale.ROOT),id=args[2],file=args[3],targetId=args[4].toLowerCase(Locale.ROOT);ImportTarget target=importTarget(active,targetId);if(target==null){sendLater(sender,"Unknown import target.");return;}
        var root=active.importsDirectory();CsvImportSource.load(root,root.resolve(file),scheduler).whenComplete((source,loadFailure)->{if(loadFailure!=null){reply(sender,loadFailure,()->"");return;}ImportService imports=active.services().require(ImportService.class);
            if(action.equals("preview")){Map<String,String>mapping=new java.util.LinkedHashMap<>();for(int i=5;i<args.length;i++){int split=args[i].indexOf('=');if(split>0)mapping.put(args[i].substring(0,split),args[i].substring(split+1));}
                imports.preview(id,source,target,mapping).whenComplete((preview,failure)->reply(sender,failure,()->"Preview rows="+preview.totalRows()+", valid="+preview.validRows()+", issues="+preview.issues().size()));}
            else if(action.equals("execute"))imports.execute(id,source,target,250).whenComplete((run,failure)->reply(sender,failure,()->"Import "+run.job().status()+": applied="+run.job().appliedRows()+", verified="+run.job().verifiedRows()+", issues="+run.issues().size()));
            else if(action.equals("reconcile"))imports.reconcile(id,source,target,250).whenComplete((run,failure)->reply(sender,failure,()->"Reconciliation "+run.job().status()+": issues="+run.issues().size()));
            else sendLater(sender,"Unknown import action.");});}
    private static ImportTarget importTarget(MagicCoreRuntime active,String id){return switch(id){case "profiles"->ImportTargets.profiles(active.services().require(com.magicstudios.magiccore.modules.profiles.PlayerProfileService.class));
        case "balances"->ImportTargets.balances(active.services().require(com.magicstudios.magiccore.modules.economy.EconomyService.class));case "ranks"->ImportTargets.ranks(active.services().require(com.magicstudios.magiccore.ranks.RankService.class));
        case "crate-keys"->ImportTargets.crateKeys(active.services().require(CrateService.class));default->null;};}
    private void leaderboard(CommandSender sender,MagicCoreRuntime active,String[]args){String type=args.length>1?args[1].toLowerCase(Locale.ROOT):"kills";CompetitiveLeaderboardService boards=active.services().require(CompetitiveLeaderboardService.class);
        switch(type){case "kills","deaths","playtime"->{StatsMetric metric=type.equals("kills")?StatsMetric.KILLS:type.equals("deaths")?StatsMetric.DEATHS:StatsMetric.PLAYTIME;boards.stats(metric,10).whenComplete((values,failure)->reply(sender,failure,()->type+": "+values));}
            case "hearts"->boards.hearts(10).whenComplete((values,failure)->reply(sender,failure,()->"hearts: "+values.stream().map(value->value.playerId()+"="+value.hearts()).toList()));
            case "wealth"->{String currency=args.length>2?args[2]:active.configuration().economy().primaryCurrency();boards.wealth(currency,10).whenComplete((values,failure)->reply(sender,failure,()->"wealth: "+values));}
            default->sendLater(sender,"Usage: /magic leaderboard <kills|deaths|playtime|hearts|wealth [currency]>");}}

    private void shards(CommandSender sender,MagicCoreRuntime active,String[]args){ShardService shards=active.services().require(ShardService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"balance";
        if(action.equals("adjust")){if(!sender.hasPermission("magiccore.admin.shards")){sendLater(sender,"You do not have permission to adjust shards.");return;}if(args.length<5){sendLater(sender,"Usage: /magic shards adjust <online-player|uuid> <delta> <reason>");return;}UUID target=targetId(sender,args[2]);if(target==null)return;long delta=Long.parseLong(args[3]);String reason=String.join(" ",java.util.Arrays.copyOfRange(args,4,args.length));shards.adjust(target,delta,reason,operation("shards-adjust")).whenComplete((value,failure)->reply(sender,failure,()->"Shard balance: "+value.amount()));return;}
        UUID target;if(args.length>2){if(!sender.hasPermission("magiccore.admin.shards")){sendLater(sender,"You do not have permission to inspect another player's shards.");return;}target=targetId(sender,args[2]);}else{Player player=requirePlayer(sender);if(player==null)return;target=player.getUniqueId();}if(target==null)return;
        if(action.equals("history")){shards.history(target,10).whenComplete((values,failure)->reply(sender,failure,()->"Shard history: "+values.stream().map(value->value.delta()+" ("+value.reason()+")").toList()));return;}
        shards.balance(target).whenComplete((value,failure)->reply(sender,failure,()->"Shards: "+value.amount()+"; earned today: "+value.earnedToday()+"/"+active.configuration().afk().policy().dailyCap()));}

    private void presentation(CommandSender sender,MagicCoreRuntime active,boolean server){Player player=requirePlayer(sender);if(player==null)return;PresentationService service=active.services().require(PresentationService.class);
        (server?service.serverNavigation(player.getUniqueId()):service.info(player.getUniqueId())).whenComplete((view,failure)->reply(sender,failure,()->(server?"Server navigation: ":"Information: ")+view.entries().stream().map(entry->entry.title()+" — "+entry.description()+" ["+entry.action()+"]").toList()));}

    private void application(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;ApplicationKind kind=args.length>1&&args[1].equalsIgnoreCase("staff")?ApplicationKind.STAFF:ApplicationKind.MEDIA;
        active.services().require(PresentationService.class).application(player.getUniqueId(),kind).whenComplete((view,failure)->{if(failure!=null){reply(sender,failure,()->"");return;}sendLater(sender,view.title()+": "+view.satisfiedRequirements()+"/"+view.totalRequirements()+" requirements met");
            for(var requirement:view.requirements())sendLater(sender,(requirement.satisfied()?"[PASS] ":"[WAIT] ")+requirement.label()+": "+requirement.detail());
            if(view.eligible())scheduler.executeEntity(player,()->player.sendMessage(Component.text("Apply now: "+view.applyUrl()).clickEvent(ClickEvent.openUrl(view.applyUrl()))),()->{});});}

    private void profile(CommandSender sender,MagicCoreRuntime active,String[]args){Player viewer=requirePlayer(sender);if(viewer==null)return;boolean administrative=args.length>1&&args[1].equalsIgnoreCase("admin");
        String targetInput=administrative?(args.length>2?args[2]:viewer.getUniqueId().toString()):(args.length>1?args[1]:viewer.getUniqueId().toString());UUID target=targetId(sender,targetInput);if(target==null)return;
        ProfileViewService profiles=active.services().require(ProfileViewService.class);(administrative?profiles.administrativeView(viewer.getUniqueId(),target):profiles.view(viewer.getUniqueId(),target)).whenComplete((view,failure)->reply(sender,failure,()->{
            if(!view.visible())return "That profile is private or you lack the required capability.";String summary=view.currentName()+" — rank="+view.rankId()+", kills="+view.kills()+", deaths="+view.deaths()+", playtime="+view.playtimeSeconds()+"s, shards="+view.shards();
            if(view.administrative())summary+=", audit-links="+view.auditEventIds()+", transaction-links="+view.economyTransactionIds();return summary;}));}

    private void spawnStash(CommandSender sender,MagicCoreRuntime active,String[]args){Player actor=requirePlayer(sender);if(actor==null)return;active.services().require(CapabilityService.class).has(actor.getUniqueId(),"SPAWNSTASH_MANAGE").whenComplete((allowed,failure)->{
        if(failure!=null){reply(sender,failure,()->"");return;}if(!allowed){sendLater(sender,"You do not have the SpawnStash staff capability.");return;}scheduler.executeEntity(actor,()->spawnStashAuthorized(actor,active,args),()->{});});}

    private void spawnStashAuthorized(Player actor,MagicCoreRuntime active,String[]args){SpawnStashController controller=active.services().require(SpawnStashController.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        if(action.equals("list")){sendLater(actor,"Open SpawnStash cases: "+controller.openCases().stream().map(value->value.id()+" target="+value.targetId()+" status="+value.status()+" signals="+value.signals().size()).toList());return;}
        if(action.equals("preview")||action.equals("create")){if(args.length<3){sendLater(actor,"Usage: /spawnstash <preview|create> <online-player>");return;}Player target=actor.getServer().getPlayerExact(args[2]);if(target==null){sendLater(actor,"Target must be online and selected by exact name.");return;}
            if(action.equals("preview"))controller.preview(actor,target).whenComplete((positions,failure)->reply(actor,failure,()->"Previewed "+positions.size()+" candidate positions; no case was created."));
            else controller.create(actor,target).whenComplete((value,failure)->reply(actor,failure,()->"SpawnStash case "+value.id()+" active with "+value.blocks().size()+" protected decoys."));return;}
        if(args.length<3){sendLater(actor,"Usage: /spawnstash <review|note|close|remove|relocate> <case-id> ...");return;}UUID caseId;try{caseId=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sendLater(actor,"Invalid case UUID.");return;}
        if(action.equals("review")){controller.find(caseId).whenComplete((found,failure)->reply(actor,failure,()->found.map(value->"Case "+value.id()+": target="+value.targetId()+", status="+value.status()+", outcome="+value.outcome()+", signals="+value.signals().stream().map(signal->signal.type()+"@"+signal.occurredAt()).toList()+", notes="+value.notes().stream().map(note->note.actorName()+": "+note.text()).toList()).orElse("Case not found.")));return;}
        if(action.equals("note")){if(args.length<4){sendLater(actor,"Usage: /spawnstash note <case-id> <note>");return;}controller.addNote(caseId,actor,String.join(" ",java.util.Arrays.copyOfRange(args,3,args.length))).whenComplete((value,failure)->reply(actor,failure,()->"Note added to "+value.id()));return;}
        if(action.equals("remove")){String note=args.length>3?String.join(" ",java.util.Arrays.copyOfRange(args,3,args.length)):"Removed by staff";controller.cleanup(caseId,SpawnStashCase.Outcome.CANCELLED,actor,note).whenComplete((value,failure)->reply(actor,failure,()->"Case removed and original blocks restored."));return;}
        if(action.equals("close")){if(args.length<5){sendLater(actor,"Usage: /spawnstash close <case-id> <NO_FINDING|CONFIRMED|FALSE_POSITIVE> <note>");return;}SpawnStashCase.Outcome outcome;try{outcome=SpawnStashCase.Outcome.valueOf(args[3].toUpperCase(Locale.ROOT));}catch(IllegalArgumentException invalid){sendLater(actor,"Outcome must be NO_FINDING, CONFIRMED, or FALSE_POSITIVE.");return;}if(!Set.of(SpawnStashCase.Outcome.NO_FINDING,SpawnStashCase.Outcome.CONFIRMED,SpawnStashCase.Outcome.FALSE_POSITIVE).contains(outcome)){sendLater(actor,"Outcome must be NO_FINDING, CONFIRMED, or FALSE_POSITIVE.");return;}String note=String.join(" ",java.util.Arrays.copyOfRange(args,4,args.length));controller.cleanup(caseId,outcome,actor,note).whenComplete((value,failure)->reply(actor,failure,()->"Case closed as "+value.outcome()+" and original blocks restored."));return;}
        if(action.equals("relocate")){if(args.length<4){sendLater(actor,"Usage: /spawnstash relocate <case-id> <online-player>");return;}Player target=actor.getServer().getPlayerExact(args[3]);if(target==null){sendLater(actor,"Target must be online and selected by exact name.");return;}controller.cleanup(caseId,SpawnStashCase.Outcome.CANCELLED,actor,"Relocated by staff").thenCompose(ignored->controller.create(actor,target)).whenComplete((value,failure)->reply(actor,failure,()->"Relocated to new case "+value.id()));return;}
        sendLater(actor,"Usage: /spawnstash <preview|create|list|review|note|close|remove|relocate>");}

    private void worth(CommandSender sender,MagicCoreRuntime active){Player player=requirePlayer(sender);if(player==null)return;scheduler.executeEntity(player,()->{var item=player.getInventory().getItemInMainHand();if(item.getType().isAir()){sendLater(player,"Hold an item to inspect its worth.");return;}ItemValuationService service=active.services().require(ItemValuationService.class);var value=service.value(BukkitValuationInputs.from(item));String rendered=service.render(value);player.sendMessage(Component.text(rendered));active.services().require(PlayerSettingsService.class).get(player.getUniqueId()).thenAccept(settings->{if(settings.enabled(PlayerSetting.WORTH_DISPLAY))scheduler.executeEntity(player,()->player.sendActionBar(Component.text(rendered)),()->{});});},()->{});}

    private void billford(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;BillfordService service=active.services().require(BillfordService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";if(action.equals("list")){sendLater(player,"Billford recipes: "+service.recipes().stream().map(recipe->recipe.id()+" — "+recipe.displayName()).toList());return;}if(args.length<3){sendLater(player,"Usage: /magic billford <preview|trade|reconcile> <recipe-or-exchange-id>");return;}if(action.equals("preview")){service.preview(player.getUniqueId(),args[2]).whenComplete((preview,failure)->reply(player,failure,()->preview.recipe().displayName()+": stock="+preview.remainingStock()+", claims="+preview.playerClaims()+", status="+preview.code()+", rewards="+preview.possibleRewards().stream().map(reward->reward.id()+" weight="+reward.weight()).toList()));return;}if(action.equals("trade")){scheduler.executeEntity(player,()->{ItemValuationService valuation=active.services().require(ItemValuationService.class);List<BillfordCandidate>snapshot=java.util.Arrays.stream(player.getInventory().getStorageContents()).filter(item->item!=null&&!item.getType().isAir()).map(item->{var input=BukkitValuationInputs.from(item);return new BillfordCandidate(input.itemId(),input.fingerprint(),input.amount(),valuation.value(input).code().equals("PROTECTED_ITEM"));}).toList();service.exchange(player.getUniqueId(),args[2],snapshot,operation("billford-exchange")).whenComplete((exchange,failure)->{if(failure==null)active.services().find(PhaseTwoPlayerListener.class).ifPresent(listener->listener.deliverPending(player));reply(player,failure,()->"Billford exchange "+exchange.id()+" settled; reward="+exchange.reward().id());});},()->{});return;}if(action.equals("reconcile")){if(args.length<4){sendLater(player,"Usage: /magic billford reconcile <exchange-id> <confirmed|not-removed>");return;}UUID id;try{id=UUID.fromString(args[2]);}catch(IllegalArgumentException invalid){sendLater(player,"Invalid exchange UUID.");return;}active.services().require(CapabilityService.class).has(player.getUniqueId(),"BILLFORD_RECONCILE").whenComplete((allowed,failure)->{if(failure!=null){reply(player,failure,()->"");return;}if(!allowed){sendLater(player,"You do not have the Billford reconciliation capability.");return;}service.reconcile(id,args[3].equalsIgnoreCase("confirmed"),operation("billford-reconcile")).whenComplete((exchange,reconcileFailure)->reply(player,reconcileFailure,()->"Reconciled exchange to "+exchange.status()));});return;}sendLater(player,"Usage: /magic billford <list|preview|trade|reconcile>");}

    private void customTool(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player actor = requirePlayer(sender);
        if (actor == null) return;
        var service = active.services().require(com.magicstudios.magiccore.modules.tools.CustomToolService.class);
        var controller = active.services().require(CustomToolController.class);
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (action.equals("list")) {
            sendLater(actor, "Tools: " + service.definitions().stream()
                    .map(value -> value.id() + " levels=" + value.upgrades().size()).toList());
            return;
        }
        if (action.equals("upgrade") && args.length > 2) {
            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException invalid) {
                sendLater(actor, "Upgrade level must be a number.");
                return;
            }
            active.services().require(CapabilityService.class).has(actor.getUniqueId(), "TOOLS_ADMIN")
                    .whenComplete((allowed, failure) -> {
                        if (failure != null) {
                            reply(actor, failure, () -> "");
                            return;
                        }
                        if (!allowed) {
                            sendLater(actor, "You do not have the tools administration capability.");
                            return;
                        }
                        scheduler.executeEntity(actor, () -> sendLater(actor,
                                controller.upgrade(actor.getInventory().getItemInMainHand(), level)
                                        ? "Tool upgraded to level " + level
                                        : "Hold a MagicCore custom tool."), () -> {});
                    });
            return;
        }
        if (action.equals("give") && args.length > 3) {
            Player target = actor.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sendLater(actor, "Target must be online.");
                return;
            }
            int level;
            try {
                level = args.length > 4 ? Integer.parseInt(args[4]) : 1;
            } catch (NumberFormatException invalid) {
                sendLater(actor, "Upgrade level must be a number.");
                return;
            }
            active.services().require(CapabilityService.class).has(actor.getUniqueId(), "TOOLS_ADMIN")
                    .whenComplete((allowed, failure) -> {
                        if (failure != null) {
                            reply(actor, failure, () -> "");
                            return;
                        }
                        if (!allowed) {
                            sendLater(actor, "You do not have the tools administration capability.");
                            return;
                        }
                        scheduler.executeEntity(target, () -> {
                            var leftovers = target.getInventory().addItem(controller.create(args[3], level));
                            sendLater(actor, leftovers.isEmpty() ? "Tool delivered." : "Target inventory is full.");
                        }, () -> {});
                    });
            return;
        }
        sendLater(actor, "Usage: /magic tool <list|upgrade level|give player tool-id [level]>");
    }

    private void reset(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player actor = requirePlayer(sender); if (actor == null) return;
        active.services().require(CapabilityService.class).has(actor.getUniqueId(), "RESET_STATS").whenComplete((allowed, failure) -> {
            if (failure != null) { reply(actor, failure, () -> ""); return; }
            if (!allowed) { sendLater(actor, "You do not have the stats reset capability."); return; }
            ResetAdminService service = active.services().require(ResetAdminService.class);
            String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "help";
            try {
                if (action.equals("preview") && args.length > 3 && args[2].equalsIgnoreCase("player")) {
                    UUID target = targetId(actor, args[3]); if (target == null) return;
                    Set<String> scopes = Set.of(args.length > 4 ? args[4] : "STATISTICS");
                    service.previewPlayer(actor.getUniqueId(), target, scopes).whenComplete((job, previewFailure) ->
                            reply(actor, previewFailure, () -> "Reset preview " + job.id() + ": records=" + job.estimatedRecords()
                                    + ", confirm with /magic reset confirm " + job.id() + " " + job.confirmationToken()));
                    return;
                }
                if (action.equals("preview") && args.length > 2 && args[2].equalsIgnoreCase("server")) {
                    int batch = args.length > 3 ? Integer.parseInt(args[3]) : 100;
                    service.previewServer(actor.getUniqueId(), Set.of("STATISTICS"), Set.of(), batch).whenComplete((job, previewFailure) ->
                            reply(actor, previewFailure, () -> "Server reset preview " + job.id() + ": records=" + job.estimatedRecords()
                                    + ", batch=" + job.batchSize() + ", confirm with /magic reset confirm " + job.id() + " " + job.confirmationToken()));
                    return;
                }
                if (action.equals("confirm") && args.length > 3) {
                    UUID id = UUID.fromString(args[2]);
                    service.confirm(id, args[3], operation("reset-confirm")).whenComplete((job, confirmFailure) ->
                            reply(actor, confirmFailure, () -> "Reset " + job.id() + " complete; processed=" + job.processedRecords()));
                    return;
                }
                if (action.equals("resume") && args.length > 2) {
                    UUID id = UUID.fromString(args[2]);
                    service.resume(id, operation("reset-resume")).whenComplete((job, resumeFailure) ->
                            reply(actor, resumeFailure, () -> "Reset " + job.id() + " status=" + job.status() + ", processed=" + job.processedRecords()));
                    return;
                }
                if (action.equals("status") && args.length > 2) {
                    UUID id = UUID.fromString(args[2]);
                    service.find(id).whenComplete((job, findFailure) -> reply(actor, findFailure,
                            () -> job.map(value -> "Reset " + value.id() + ": " + value.status() + " " + value.processedRecords() + "/" + value.estimatedRecords()).orElse("Reset not found.")));
                    return;
                }
            } catch (IllegalArgumentException invalid) { sendLater(actor, "Invalid reset argument: " + invalid.getMessage()); return; }
            sendLater(actor, "Usage: /magic reset <preview player target [scope]|preview server [batch]|confirm id token|resume id|status id>");
        });
    }

    private void keyall(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player actor = requirePlayer(sender); if (actor == null) return;
        active.services().require(CapabilityService.class).has(actor.getUniqueId(), "KEYALL_MANAGE").whenComplete((allowed, failure) -> {
            if (failure != null) { reply(actor, failure, () -> ""); return; }
            if (!allowed) { sendLater(actor, "You do not have the keyall management capability."); return; }
            KeyallService service = active.services().require(KeyallService.class); KeyallController controller = active.services().require(KeyallController.class);
            String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
            try {
                if (action.equals("list")) { sendLater(actor, "Keyalls: " + service.definitions().values().stream().map(value -> value.id() + " key=" + value.keyId() + " amount=" + value.amount() + " audience=" + value.audience()).toList()); return; }
                if (action.equals("preview") && args.length > 2) { controller.preview(args[2].toUpperCase(Locale.ROOT), com.magicstudios.magiccore.modules.keyall.KeyallRun.Trigger.MANUAL).whenComplete((run, previewFailure) -> reply(actor, previewFailure, () -> "Keyall preview " + run.id() + " recipients=" + run.recipients().size())); return; }
                if (action.equals("run") && args.length > 2) { controller.runManual(args[2].toUpperCase(Locale.ROOT), operation("keyall-manual")).whenComplete((run, runFailure) -> reply(actor, runFailure, () -> "Keyall " + run.id() + " " + run.status() + "; delivered=" + run.delivered() + ", failed=" + run.failures().size())); return; }
                if (action.equals("execute") && args.length > 2) { UUID id = UUID.fromString(args[2]); service.execute(id, operation("keyall-execute")).whenComplete((run, runFailure) -> reply(actor, runFailure, () -> "Keyall " + run.id() + " " + run.status() + "; delivered=" + run.delivered())); return; }
                if (action.equals("cancel") && args.length > 2) { UUID id = UUID.fromString(args[2]); service.cancel(id, operation("keyall-cancel")).whenComplete((run, cancelFailure) -> reply(actor, cancelFailure, () -> "Keyall " + run.id() + " cancelled.")); return; }
                if (action.equals("threshold") && args.length > 3) { long amount = Long.parseLong(args[3]); controller.contribute(args[2].toUpperCase(Locale.ROOT), amount, operation("keyall-threshold")).whenComplete((run, thresholdFailure) -> reply(actor, thresholdFailure, () -> run.map(value -> "Threshold triggered keyall " + value.id() + " status=" + value.status()).orElse("Threshold contribution recorded."))); return; }
            } catch (IllegalArgumentException invalid) { sendLater(actor, "Invalid keyall argument: " + invalid.getMessage()); return; }
            sendLater(actor, "Usage: /magic keyall <list|preview id|run id|execute run-id|cancel run-id|threshold id amount>");
        });
    }

    private void gemshop(CommandSender sender, MagicCoreRuntime active, String[] args) {
        Player player=requirePlayer(sender);if(player==null)return;GemShopService service=active.services().require(GemShopService.class);
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        try{
            if(action.equals("list")){var products=args.length>2?service.products(args[2]):service.products();sendLater(player,"GemShop: "+products.stream().map(value->value.id()+" ["+value.category()+"] "+value.priceMinor()+" gems").toList());return;}
            if(action.equals("quote")&&args.length>2){service.quote(player.getUniqueId(),args[2].toUpperCase(Locale.ROOT),operation("gemshop-quote")).whenComplete((quote,failure)->reply(player,failure,()->"Quote "+quote.id()+": "+quote.product().displayName()+" costs "+quote.product().priceMinor()+" gems; confirm with /magic gemshop confirm "+quote.id()));return;}
            if(action.equals("confirm")&&args.length>2){UUID quoteId=UUID.fromString(args[2]);service.confirm(player.getUniqueId(),quoteId,operation("gemshop-confirm")).whenComplete((receipt,failure)->{if(failure==null)active.services().find(PhaseTwoPlayerListener.class).ifPresent(listener->listener.deliverPending(player));reply(player,failure,()->"GemShop purchase complete; receipt="+receipt.id()+", balance="+receipt.balanceAfterMinor());});return;}
        }catch(IllegalArgumentException invalid){sendLater(player,"Invalid GemShop argument: "+invalid.getMessage());return;}
        sendLater(player,"Usage: /magic gemshop <list [category]|quote product|confirm quote-id>");
    }

    private void gems(CommandSender sender,MagicCoreRuntime active,String[]args){Player actor=requirePlayer(sender);if(actor==null)return;EconomyService economy=active.services().require(EconomyService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"balance";
        if(action.equals("balance")){UUID target=args.length>2?targetId(actor,args[2]):actor.getUniqueId();if(target==null)return;economy.balance(target,"GEMS").whenComplete((balance,failure)->reply(actor,failure,()->"Gem balance: "+balance.minorUnits()));return;}
        if(action.equals("adjust")&&args.length>3){UUID target=targetId(actor,args[2]);if(target==null)return;long amount;try{amount=Long.parseLong(args[3]);}catch(NumberFormatException invalid){sendLater(actor,"Gem adjustment must be an integer.");return;}active.services().require(CapabilityService.class).has(actor.getUniqueId(),"MANAGE_ECONOMY").whenComplete((allowed,failure)->{if(failure!=null){reply(actor,failure,()->"");return;}if(!allowed){sendLater(actor,"You do not have economy management capability.");return;}economy.adjust(target,new Money("GEMS",amount),actor.getUniqueId().toString(),args.length>4?String.join(" ",java.util.Arrays.copyOfRange(args,4,args.length)):"admin gem adjustment",operation("gems-adjust")).whenComplete((mutation,adjustFailure)->reply(actor,adjustFailure,()->"Gem balance adjusted to "+mutation.resultingBalance().minorUnits()));});return;}
        sendLater(actor,"Usage: /magic gems <balance [player]|adjust player amount [reason]>");}

    private void discord(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;var integration=active.services().find(DiscordIntegrationService.class);if(integration.isEmpty()){sendLater(player,"Discord integration is disabled.");return;}String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";
        if(action.equals("status")){integration.get().linkedDiscordId(player.getUniqueId()).whenComplete((id,failure)->reply(player,failure,()->id.map(value->"Linked to Discord ID "+value+" via "+integration.get().provider()).orElse("Discord account is not linked.")));return;}
        var bridge=active.services().find(CustomDiscordBridge.class);if(bridge.isEmpty()){sendLater(player,"Link codes are managed by "+integration.get().provider()+".");return;}
        if(action.equals("link")){bridge.get().issueLinkCode(player.getUniqueId(),operation("discord-link-code")).whenComplete((issue,failure)->reply(player,failure,()->"Discord link code: "+issue.code()+" (expires "+issue.expiresAt()+")"));return;}
        if(action.equals("unlink")){bridge.get().unlink(player.getUniqueId(),operation("discord-unlink")).whenComplete((changed,failure)->reply(player,failure,()->changed?"Discord account unlinked.":"No active Discord link."));return;}
        sendLater(player,"Usage: /magic discord <status|link|unlink>");}

    private void menu(CommandSender sender,MagicCoreRuntime active,String[]args){Player player=requirePlayer(sender);if(player==null)return;MagicGuiController menus=active.services().require(MagicGuiController.class);menus.open(player,args.length>1?args[1].toLowerCase(Locale.ROOT):"main");}

    private void koth(CommandSender sender,MagicCoreRuntime active,String[]args){KothService service=active.services().require(KothService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";if(action.equals("status")){var futures=service.definitions().stream().map(value->service.active(value.id()).toCompletableFuture()).toList();java.util.concurrent.CompletableFuture.allOf(futures.toArray(java.util.concurrent.CompletableFuture[]::new)).whenComplete((ignored,failure)->reply(sender,failure,()->"KOTH: "+java.util.stream.IntStream.range(0,futures.size()).mapToObj(index->{var definition=service.definitions().get(index);return definition.id()+"="+futures.get(index).join().map(run->run.status()+" holder="+run.holdingName()+" progress="+run.capturedMillis()+"ms").orElse("INACTIVE");}).toList()));return;}if(action.equals("history")){service.recent(20).whenComplete((values,failure)->reply(sender,failure,()->"KOTH history: "+values.stream().map(value->value.definitionId()+":"+value.status()+":"+value.winnerName()).toList()));return;}eventAuthorization(sender,active).whenComplete((allowed,failure)->{if(failure!=null){reply(sender,failure,()->"");return;}if(!allowed){sendLater(sender,"You do not have event management capability.");return;}try{if(action.equals("start")&&args.length>2){service.start(args[2].toUpperCase(Locale.ROOT),operation("koth-start")).whenComplete((run,startFailure)->reply(sender,startFailure,()->"Started KOTH "+run.definitionId()+" run "+run.id()));return;}if(action.equals("cancel")&&args.length>2){service.cancel(UUID.fromString(args[2]),operation("koth-cancel")).whenComplete((run,cancelFailure)->reply(sender,cancelFailure,()->"KOTH "+run.id()+" is "+run.status()));return;}}catch(IllegalArgumentException invalid){sendLater(sender,"Invalid KOTH argument: "+invalid.getMessage());return;}sendLater(sender,"Usage: /magic koth <status|history|start id|cancel run-id>");});}

    private void vote(CommandSender sender,MagicCoreRuntime active,String[]args){VotePartyService service=active.services().require(VotePartyService.class);String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"status";if(action.equals("status")){var state=service.state().toCompletableFuture();var party=service.activeParty().toCompletableFuture();java.util.concurrent.CompletableFuture.allOf(state,party).whenComplete((ignored,failure)->reply(sender,failure,()->"Vote party: count="+state.join().count()+", active="+party.join().map(value->value.id()+" hits="+value.totalHits()+"/"+value.maximumHits()).orElse("none")));return;}eventAuthorization(sender,active).whenComplete((allowed,failure)->{if(failure!=null){reply(sender,failure,()->"");return;}if(!allowed){sendLater(sender,"You do not have event management capability.");return;}try{if(action.equals("submit")&&args.length>2){UUID target=targetId(sender,args[2]);if(target==null)return;String voteService=args.length>3?args[3]:"ADMIN_VERIFIED";active.services().require(PinataController.class).acceptVerifiedVote("admin:"+UUID.randomUUID(),target,voteService,java.time.Instant.now(),sender.getServer().getPlayer(target)!=null).whenComplete((outcome,submitFailure)->reply(sender,submitFailure,()->"Verified vote recorded; count="+outcome.state().count()+", party-triggered="+outcome.triggeredParty().isPresent()));return;}if(action.equals("cancel")&&args.length>2){service.cancel(UUID.fromString(args[2]),operation("pinata-cancel")).whenComplete((party,cancelFailure)->reply(sender,cancelFailure,()->"Pinata "+party.id()+" is "+party.status()));return;}}catch(IllegalArgumentException invalid){sendLater(sender,"Invalid vote-party argument: "+invalid.getMessage());return;}sendLater(sender,"Usage: /magic vote <status|submit player [service]|cancel party-id>");});}
    private java.util.concurrent.CompletionStage<Boolean>eventAuthorization(CommandSender sender,MagicCoreRuntime active){return sender instanceof Player player?active.services().require(CapabilityService.class).has(player.getUniqueId(),"EVENTS_MANAGE"):java.util.concurrent.CompletableFuture.completedFuture(true);}

    private UUID targetId(CommandSender sender,String input){try{return UUID.fromString(input);}catch(IllegalArgumentException ignored){}Player target=sender.getServer().getPlayerExact(input);if(target!=null)return target.getUniqueId();sendLater(sender,"Target must be an online exact player name or UUID.");return null;}

    private void teleport(MagicCoreRuntime active, Player player, WorldPosition position) {
        scheduler.executeGlobal(() -> {
            World world = player.getServer().getWorld(position.worldId());
            if (world == null) { sendLater(player, "Destination world is unavailable."); return; }
            Location location = new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
            active.services().require(TeleportService.class).teleport(player, location, teleportWarmup(active), operation("teleport"))
                    .whenComplete((value, failure) -> reply(player, failure, value::code));
        });
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Component.text("This subcommand requires a player.")); return null;
    }

    private void reply(CommandSender sender, Throwable failure, java.util.function.Supplier<String> success) {
        sendLater(sender, failure == null ? success.get() : "Action rejected: " + rootMessage(failure));
    }
    private static String rootMessage(Throwable failure) {
        Throwable current = failure; while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
    private static String operation(String prefix) { return "command:" + prefix + ":" + UUID.randomUUID(); }
    private static Duration teleportWarmup(MagicCoreRuntime runtime) {
        return Duration.ofSeconds(runtime.configuration().essentials().teleport().warmupSeconds());
    }

    private void admin(CommandSender sender, MagicCoreRuntime active, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("feature")) {
            sender.sendMessage(Component.text("Usage: /magic admin feature <id> <INTERNAL|EXTERNAL|DISABLED>"));
            return;
        }
        AdminEditingService admin = active.services().require(AdminEditingService.class);
        AdminActor actor = sender instanceof Player player
                ? new AdminActor(player.getUniqueId(), player.getName(), false) : AdminActor.consoleActor();
        ProviderMode mode = ProviderMode.valueOf(args[3].toUpperCase(Locale.ROOT));
        long revision = admin.featuresSnapshot().revision();
        admin.setFeatureMode(actor, args[2], mode, revision, "command:" + java.util.UUID.randomUUID())
                .whenComplete((result, failure) -> sendLater(sender, failure == null
                        ? "features.yml revision " + result.commit().snapshot().revision() + " committed; restart required"
                        : "Admin change rejected: " + failure.getMessage()));
    }

    private void setup(CommandSender sender, MagicCoreRuntime active, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("/magic setup requires a player session; use YAML plus /magic reload from console."));
            return;
        }
        SetupService setup = active.services().require(SetupService.class);
        if (args.length >= 3 && args[1].equalsIgnoreCase("storage")) {
            var plan = setup.selectStorage(player.getUniqueId(), args[2]);
            sender.sendMessage(Component.text("Setup storage selected: " + plan.storageProvider()));
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("review")) {
            sender.sendMessage(Component.text("Setup review: " + setup.review(player.getUniqueId())));
        } else {
            SetupPreset preset = args.length >= 2 ? SetupPreset.valueOf(args[1].toUpperCase(Locale.ROOT)) : SetupPreset.CUSTOM;
            sender.sendMessage(Component.text("Setup started: " + setup.begin(player.getUniqueId(), preset)));
        }
    }

    private void sendLater(CommandSender sender, String message) {
        if (sender instanceof Entity entity) scheduler.executeEntity(entity,
                () -> sender.sendMessage(Component.text(message)), () -> { });
        else scheduler.executeGlobal(() -> sender.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        if(command.getName().equalsIgnoreCase("spawnstash")){if(args.length==1)return List.of("preview","create","list","review","note","close","remove","relocate");if(args.length==3&&args[0].equalsIgnoreCase("close"))return List.of("NO_FINDING","CONFIRMED","FALSE_POSITIVE");return List.of();}
        if (args.length == 1) return List.of("menu", "koth", "vote", "setup", "admin", "diagnose", "reload", "info", "server", "apply", "profile", "spawnstash", "worth", "billford", "tool", "reset", "keyall", "gemshop", "gems", "discord", "home", "warp", "tpa",
                "back", "rtp", "spawn", "setspawn", "kit", "settings", "shop", "sell", "pwarp", "auction", "order", "bounty", "market", "hearts", "combat", "crate", "stats", "store", "import", "leaderboard", "shards", "afk");
        if (args.length == 2 && args[0].equalsIgnoreCase("setup"))
            return List.of("CUSTOM", "ECONOMY_SMP", "LIFESTEAL_SMP", "DONUT_LIKE", "storage", "review");
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) return List.of("feature");
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("feature"))
            return List.of("INTERNAL", "EXTERNAL", "DISABLED");
        if (args.length == 2 && args[0].equalsIgnoreCase("home")) return List.of("list", "set", "delete");
        if (args.length == 2 && args[0].equalsIgnoreCase("tpa")) return List.of("here", "accept", "deny", "cancel");
        if (args.length == 2 && args[0].equalsIgnoreCase("pwarp")) return List.of("list","search","create","delete","favorite","sponsor","unsponsor","transfer","moderate");
        if(args.length==4&&args[0].equalsIgnoreCase("pwarp")&&args[1].equalsIgnoreCase("favorite"))return List.of("true","false");
        if(args.length==4&&args[0].equalsIgnoreCase("pwarp")&&args[1].equalsIgnoreCase("moderate"))return List.of("ACTIVE","PENDING_REVIEW","SUSPENDED");
        if (args.length == 2 && args[0].equalsIgnoreCase("shop")) return List.of("list");
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) return List.of("hand","all","category","history","receipt","reconcile");
        if (args.length == 2 && args[0].equalsIgnoreCase("billford")) return List.of("list","preview","trade","reconcile");
        if (args.length == 2 && args[0].equalsIgnoreCase("tool")) return List.of("list","upgrade","give");
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return List.of("preview","confirm","resume","status");
        if (args.length == 2 && args[0].equalsIgnoreCase("keyall")) return List.of("list","preview","run","execute","cancel","threshold");
        if (args.length == 2 && args[0].equalsIgnoreCase("gemshop")) return List.of("list","quote","confirm");
        if (args.length == 2 && args[0].equalsIgnoreCase("gems")) return List.of("balance","adjust");
        if (args.length == 2 && args[0].equalsIgnoreCase("discord")) return List.of("status","link","unlink");
        if (args.length == 2 && args[0].equalsIgnoreCase("menu")) {MagicCoreRuntime active=runtime.get();return active==null?List.of():active.services().find(MagicGuiController.class).map(value->value.menuIds().stream().sorted().toList()).orElse(List.of());}
        if(args.length==2&&args[0].equalsIgnoreCase("koth"))return List.of("status","history","start","cancel");
        if(args.length==2&&args[0].equalsIgnoreCase("vote"))return List.of("status","submit","cancel");
        if (args.length == 3 && args[0].equalsIgnoreCase("reset") && args[1].equalsIgnoreCase("preview")) return List.of("player","server");
        if (args.length == 2 && args[0].equalsIgnoreCase("kit")) return List.of("list");
        if (args.length == 2 && args[0].equalsIgnoreCase("settings"))
            return java.util.Arrays.stream(PlayerSetting.values()).map(Enum::name).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("settings")) return List.of("true", "false");
        if (args.length == 2 && args[0].equalsIgnoreCase("auction")) return List.of("list", "sell", "buy", "cancel", "history");
        if(args.length==2&&args[0].equalsIgnoreCase("order"))return List.of("list","create","fill","cancel","history");
        if(args.length==2&&args[0].equalsIgnoreCase("bounty"))return List.of("list","create","history");
        if(args.length==2&&args[0].equalsIgnoreCase("market"))return List.of("stats","expire","top");
        if(args.length==2&&args[0].equalsIgnoreCase("hearts"))return List.of("status","top","withdraw","revive");
        if(args.length==2&&args[0].equalsIgnoreCase("combat"))return List.of("status","protection");
        if(args.length==3&&args[0].equalsIgnoreCase("combat")&&args[1].equalsIgnoreCase("protection"))return List.of("remove");
        if(args.length==2&&args[0].equalsIgnoreCase("crate"))return List.of("list","preview","keys","open","history","grant");
        if(args.length==2&&args[0].equalsIgnoreCase("stats"))return List.of("top");
        if(args.length==3&&args[0].equalsIgnoreCase("stats")&&args[1].equalsIgnoreCase("top"))return java.util.Arrays.stream(StatsMetric.values()).map(Enum::name).toList();
        if(args.length==2&&args[0].equalsIgnoreCase("store"))return List.of("link","products","goal");
        if(args.length==2&&args[0].equalsIgnoreCase("import"))return List.of("preview","execute","reconcile","status");
        if(args.length==5&&args[0].equalsIgnoreCase("import"))return List.of("profiles","balances","ranks","crate-keys");
        if(args.length==2&&args[0].equalsIgnoreCase("leaderboard"))return List.of("kills","deaths","playtime","hearts","wealth");
        if(args.length==2&&(args[0].equalsIgnoreCase("shards")||args[0].equalsIgnoreCase("afk")))return List.of("balance","history","adjust");
        if(args.length==2&&args[0].equalsIgnoreCase("apply"))return List.of("media","staff");
        if(args.length==2&&args[0].equalsIgnoreCase("profile"))return List.of("admin");
        if(args.length==2&&args[0].equalsIgnoreCase("spawnstash"))return List.of("preview","create","list","review","note","close","remove","relocate");
        return List.of();
    }
}
