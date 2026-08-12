package com.magicstudios.magiccore.bootstrap;
import com.magicstudios.magiccore.modules.bounties.BountyService;
import com.magicstudios.magiccore.modules.combat.*;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.platform.BukkitWorldPositions;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionDecision;
import com.magicstudios.magiccore.protection.ProtectionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class CombatPlayerListener implements Listener{
 private final CombatService combat;private final NewPlayerProtectionService newbies;private final TeamRelationCache teams;
 private final ProtectionService protection;private final AbilityCooldownService cooldowns;private final CombatLogoutRegistry logoutRegistry;
 private final LifestealService lifesteal;private final BountyService bounties;private final Set<String>restrictedCommands;private final Set<Material>restrictedItems;
 private final PlayerStatsService stats;
 private final Duration pearlCooldown,tridentCooldown;private final boolean removeNewbieOnAttack,friendlyFire;
 public CombatPlayerListener(CombatService combat,NewPlayerProtectionService newbies,TeamRelationCache teams,ProtectionService protection,
                             AbilityCooldownService cooldowns,CombatLogoutRegistry logoutRegistry,LifestealService lifesteal,BountyService bounties,
                             PlayerStatsService stats,
                             Set<String>restrictedCommands,Set<Material>restrictedItems,Duration pearlCooldown,Duration tridentCooldown,
                             boolean removeNewbieOnAttack,boolean friendlyFire){this.combat=combat;this.newbies=newbies;this.teams=teams;this.protection=protection;this.cooldowns=cooldowns;
  this.logoutRegistry=logoutRegistry;this.lifesteal=lifesteal;this.bounties=bounties;this.restrictedCommands=restrictedCommands;this.restrictedItems=restrictedItems;
  this.stats=stats;
  this.pearlCooldown=pearlCooldown;this.tridentCooldown=tridentCooldown;this.removeNewbieOnAttack=removeNewbieOnAttack;this.friendlyFire=friendlyFire;}
 @EventHandler public void onJoin(PlayerJoinEvent event){UUID id=event.getPlayer().getUniqueId();newbies.refresh(id);teams.refresh(id);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void onDamage(EntityDamageByEntityEvent event){if(!(event.getEntity()instanceof Player victim))return;Player attacker=attacker(event.getDamager());if(attacker==null||attacker.equals(victim))return;
  UUID attackerId=attacker.getUniqueId(),victimId=victim.getUniqueId();TeamRelationCache.Relation relation=teams.relation(attackerId,victimId);
  if(relation==TeamRelationCache.Relation.UNKNOWN){teams.refresh(attackerId);teams.refresh(victimId);deny(event,attacker,"PvP state is still loading.");return;}
  if(!friendlyFire&&relation==TeamRelationCache.Relation.SAME){deny(event,attacker,"Friendly fire is disabled.");return;}
  NewPlayerProtectionService.State attackerNewbie=newbies.state(attackerId);
  NewPlayerProtectionService.State victimNewbie=newbies.state(victimId);
  if(attackerNewbie==NewPlayerProtectionService.State.UNKNOWN||victimNewbie==NewPlayerProtectionService.State.UNKNOWN){newbies.refresh(attackerId);newbies.refresh(victimId);deny(event,attacker,"PvP protection is still loading.");return;}
  if(victimNewbie==NewPlayerProtectionService.State.PROTECTED){deny(event,attacker,"That player has new-player protection.");return;}
  if(attackerNewbie==NewPlayerProtectionService.State.PROTECTED){if(!removeNewbieOnAttack){deny(event,attacker,"You have new-player protection.");return;}
   deny(event,attacker,"New-player protection removed. Attack again to enter PvP.");newbies.remove(attackerId,"pvp-attack:"+UUID.randomUUID());return;}
  if(!allowed(protection.check(attackerId,BukkitWorldPositions.from(attacker.getLocation()),ProtectionAction.PVP))
          ||!allowed(protection.check(attackerId,BukkitWorldPositions.from(victim.getLocation()),ProtectionAction.PVP))){deny(event,attacker,"PvP is blocked in this area.");return;}
  combat.tag(attackerId,victimId);
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void onCommand(PlayerCommandPreprocessEvent event){if(!combat.isTagged(event.getPlayer().getUniqueId()))return;String[]parts=event.getMessage().substring(1).toLowerCase(Locale.ROOT).split("\\s+");
  String command=parts[0].equals("magic")||parts[0].equals("magiccore")?(parts.length>1?parts[1]:parts[0]):parts[0];if(restrictedCommands.contains(command)){event.setCancelled(true);event.getPlayer().sendMessage(Component.text("That command is disabled while combat tagged."));}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void onInteract(PlayerInteractEvent event){Material material=event.getMaterial();UUID id=event.getPlayer().getUniqueId();
  if(combat.isTagged(id)&&restrictedItems.contains(material)){event.setCancelled(true);event.getPlayer().sendMessage(Component.text("That item is restricted during combat."));return;}
  if(material==Material.ENDER_PEARL&&!cooldowns.tryUse(id,AbilityCooldownService.Ability.ENDER_PEARL,pearlCooldown)){event.setCancelled(true);event.getPlayer().sendMessage(Component.text("Ender pearl cooldown: "+cooldowns.remaining(id,AbilityCooldownService.Ability.ENDER_PEARL).toSeconds()+"s"));}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void onProjectile(ProjectileLaunchEvent event){if(event.getEntity()instanceof Trident&&event.getEntity().getShooter()instanceof Player player
   &&!cooldowns.tryUse(player.getUniqueId(),AbilityCooldownService.Ability.TRIDENT,tridentCooldown)){event.setCancelled(true);player.sendMessage(Component.text("Trident cooldown active."));}}
 @EventHandler(priority=EventPriority.HIGHEST)public void onQuit(PlayerQuitEvent event){var resolution=combat.logout(event.getPlayer().getUniqueId());if(!resolution.resolved()){newbies.invalidate(event.getPlayer().getUniqueId());teams.invalidate(event.getPlayer().getUniqueId());return;}
  logoutRegistry.mark(event.getPlayer().getUniqueId());try{if(event.getPlayer().getHealth()>0)event.getPlayer().setHealth(0);}finally{logoutRegistry.clear(event.getPlayer().getUniqueId());}
  lifesteal.transfer(resolution.kill(),"combat-logout:"+resolution.kill().eventId()+":hearts");bounties.claim(resolution.kill(),"combat-logout:"+resolution.kill().eventId()+":bounty");stats.recordKill(resolution.kill(),"combat-logout:"+resolution.kill().eventId()+":stats");
  newbies.invalidate(event.getPlayer().getUniqueId());teams.invalidate(event.getPlayer().getUniqueId());}
 private static Player attacker(org.bukkit.entity.Entity damager){if(damager instanceof Player player)return player;if(damager instanceof Projectile projectile){ProjectileSource shooter=projectile.getShooter();if(shooter instanceof Player player)return player;}return null;}
 private static boolean allowed(java.util.concurrent.CompletionStage<ProtectionDecision>stage){try{ProtectionDecision decision=stage.toCompletableFuture().getNow(null);return decision!=null&&decision.allowed();}catch(CompletionException failure){return false;}}
 private static void deny(EntityDamageByEntityEvent event,Player player,String message){event.setCancelled(true);player.sendMessage(Component.text(message));}
}
