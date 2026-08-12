package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.config.model.CombatFile;
import com.magicstudios.magiccore.integrations.vulcan.VulcanService;
import com.magicstudios.magiccore.modules.combat.FastCrystalActionObserved;
import com.magicstudios.magiccore.modules.combat.FastCrystalPolicy;
import com.magicstudios.magiccore.modules.settings.PlayerSetting;
import com.magicstudios.magiccore.modules.settings.PlayerSettingsService;
import com.magicstudios.magiccore.platform.BukkitWorldPositions;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionDecision;
import com.magicstudios.magiccore.protection.ProtectionService;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FastCrystalController implements Listener, AutoCloseable {
    private final SchedulerFacade scheduler;
    private final PlayerSettingsService settings;
    private final ProtectionService protection;
    private final VulcanService vulcan;
    private final DomainEventBus events;
    private final Clock clock;
    private final FastCrystalPolicy policy;
    private final Set<Material> baseBlocks;
    private final double maximumRange;
    private final double damage;
    private final double knockback;
    private final Sound sound;
    private final float soundVolume;
    private final float soundPitch;
    private final Map<UUID, Boolean> enabled = new ConcurrentHashMap<>();

    public FastCrystalController(SchedulerFacade scheduler, PlayerSettingsService settings,
                                 ProtectionService protection, VulcanService vulcan, DomainEventBus events,
                                 CombatFile.FastCrystal config, Clock clock) {
        this.scheduler = scheduler;
        this.settings = settings;
        this.protection = protection;
        this.vulcan = vulcan;
        this.events = events;
        this.clock = clock;
        this.policy = new FastCrystalPolicy(config.enabled(), Duration.ofMillis(config.cooldownMillis()),
                config.maximumRange(), config.requireLineOfSight(), Set.copyOf(config.worldAllowlist()));
        this.baseBlocks = config.baseBlocks().stream().map(Material::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.maximumRange = config.maximumRange();
        this.damage = config.damage();
        this.knockback = config.knockback();
        this.sound = java.util.Objects.requireNonNull(Registry.SOUNDS.get(NamespacedKey.minecraft(config.sound().toLowerCase(java.util.Locale.ROOT))));
        this.soundVolume = config.soundVolume();
        this.soundPitch = config.soundPitch();
    }

    public void refresh(Player player) {
        settings.get(player.getUniqueId()).whenComplete((value, failure) -> {
            if (failure == null) enabled.put(player.getUniqueId(), value.enabled(PlayerSetting.FAST_CRYSTAL));
        });
    }

    public boolean enabled(UUID playerId) { return enabled.getOrDefault(playerId, false); }

    @EventHandler public void onJoin(PlayerJoinEvent event) { refresh(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { enabled.remove(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getMaterial() != Material.END_CRYSTAL || event.getClickedBlock() == null
                || !enabled(event.getPlayer().getUniqueId())) return;
        Player player = event.getPlayer();
        if (!baseBlocks.contains(event.getClickedBlock().getType())) return;
        Location spawn = event.getClickedBlock().getLocation().add(.5, 1, .5);
        var decision = policy.evaluate(player.getUniqueId(), true, spawn.getWorld().getName(),
                player.getEyeLocation().distanceSquared(spawn), hasSight(player, event.getClickedBlock().getLocation()), clock.instant());
        if (decision != FastCrystalPolicy.Decision.ALLOWED) { deny(event, player, decision); return; }
        ProtectionDecision protectionDecision = protection.check(player.getUniqueId(), BukkitWorldPositions.from(spawn),
                ProtectionAction.BLOCK_PLACE).toCompletableFuture().getNow(null);
        if (protectionDecision == null || !protectionDecision.allowed()) { deny(event, player, FastCrystalPolicy.Decision.WORLD_BLOCKED); return; }
        if (!spawn.getBlock().isEmpty() || !spawn.clone().add(0, 1, 0).getBlock().isEmpty()) { event.setCancelled(true); return; }
        event.setCancelled(true);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (player.getGameMode() != GameMode.CREATIVE) held.setAmount(held.getAmount() - 1);
        spawn.getWorld().spawn(spawn, EnderCrystal.class, crystal -> crystal.setShowingBottom(false));
        observe(player, "PLACE");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal) || !(event.getDamager() instanceof Player player)
                || !enabled(player.getUniqueId())) return;
        Location location = crystal.getLocation();
        var decision = policy.evaluate(player.getUniqueId(), true, location.getWorld().getName(),
                player.getEyeLocation().distanceSquared(location), player.hasLineOfSight(crystal), clock.instant());
        if (decision != FastCrystalPolicy.Decision.ALLOWED) { event.setCancelled(true); player.sendActionBar(Component.text(label(decision))); return; }
        ProtectionDecision protectionDecision = protection.check(player.getUniqueId(), BukkitWorldPositions.from(location),
                ProtectionAction.PVP).toCompletableFuture().getNow(null);
        if (protectionDecision == null || !protectionDecision.allowed()) { event.setCancelled(true); player.sendActionBar(Component.text("Fast Crystal blocked by protection.")); return; }
        event.setCancelled(true);
        crystal.remove();
        location.getWorld().playSound(location, sound, soundVolume, soundPitch);
        for (LivingEntity target : location.getNearbyLivingEntities(maximumRange)) {
            scheduler.executeEntity(target, () -> applyImpact(player, target, location), () -> {});
        }
        observe(player, "BREAK");
    }

    private void applyImpact(Player attacker, LivingEntity target, Location origin) {
        if (!target.isValid() || target.isDead()) return;
        double distance = target.getLocation().distance(origin);
        if (distance > maximumRange) return;
        double scale = Math.max(0, 1 - distance / maximumRange);
        target.damage(damage * scale, attacker);
        Vector vector = target.getLocation().toVector().subtract(origin.toVector());
        if (vector.lengthSquared() > 0.0001) target.setVelocity(target.getVelocity().add(vector.normalize().multiply(knockback * scale).setY(Math.max(.1, knockback * scale))));
    }

    private void observe(Player player, String action) {
        int flags = vulcan.recentFlags(player.getUniqueId(), clock.instant().minusSeconds(5)).size();
        events.publish(new FastCrystalActionObserved(player.getUniqueId(), action, flags, clock.instant()));
    }

    private boolean hasSight(Player player, Location target) {
        var result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), maximumRange);
        return result == null || result.getHitBlock() == null || result.getHitBlock().getLocation().equals(target);
    }

    private static void deny(PlayerInteractEvent event, Player player, FastCrystalPolicy.Decision decision) {
        event.setCancelled(true); player.sendActionBar(Component.text(label(decision)));
    }
    private static String label(FastCrystalPolicy.Decision decision) {
        return switch (decision) {
            case COOLDOWN -> "Fast Crystal cooldown active.";
            case RANGE -> "Crystal is out of range.";
            case LINE_OF_SIGHT -> "Crystal requires line of sight.";
            case WORLD_BLOCKED, DISABLED -> "Fast Crystal is unavailable here.";
            case PLAYER_DISABLED -> "Fast Crystal is disabled.";
            case ALLOWED -> "";
        };
    }

    @Override public void close() { HandlerList.unregisterAll(this); enabled.clear(); }
}
