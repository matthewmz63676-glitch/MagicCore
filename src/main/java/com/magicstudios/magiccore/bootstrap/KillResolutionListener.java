package com.magicstudios.magiccore.bootstrap;
import com.magicstudios.magiccore.modules.bounties.BountyService;
import com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill;
import com.magicstudios.magiccore.modules.lifesteal.LifestealService;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.time.Clock;
import java.util.UUID;
public final class KillResolutionListener implements Listener{
 private final BountyService bounties;private final LifestealService lifesteal;private final PlayerStatsService stats;private final Clock clock;private final com.magicstudios.magiccore.modules.combat.CombatLogoutRegistry logoutRegistry;
 public KillResolutionListener(BountyService bounties,LifestealService lifesteal,PlayerStatsService stats,Clock clock,com.magicstudios.magiccore.modules.combat.CombatLogoutRegistry logoutRegistry){this.bounties=bounties;this.lifesteal=lifesteal;this.stats=stats;this.clock=clock;this.logoutRegistry=logoutRegistry;}
 @EventHandler public void onDeath(PlayerDeathEvent event){var victim=event.getEntity();var killer=victim.getKiller();
  if(logoutRegistry.consume(victim.getUniqueId()))return;if(killer==null){UUID eventId=UUID.randomUUID();lifesteal.nonPlayerDeath(victim.getUniqueId(),"environment-death:"+eventId);stats.recordDeath(victim.getUniqueId(),eventId,"environment-death:"+eventId);return;}
  UUID eventId=UUID.randomUUID();var kill=new VerifiedPlayerKill(eventId,killer.getUniqueId(),victim.getUniqueId(),"BUKKIT_PLAYER_DEATH_EVENT",clock.instant());
  lifesteal.transfer(kill,"kill:"+eventId+":hearts");bounties.claim(kill,"kill:"+eventId+":bounty");stats.recordKill(kill,"kill:"+eventId+":stats");}
}
