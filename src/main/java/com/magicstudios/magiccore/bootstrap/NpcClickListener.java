package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.integrations.npcs.NpcActionRequested;
import com.magicstudios.magiccore.integrations.npcs.NpcIntegrationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.time.Clock;

public final class NpcClickListener implements Listener {private final NpcIntegrationService npcs;private final DomainEventBus events;private final Clock clock;
 public NpcClickListener(NpcIntegrationService npcs,DomainEventBus events,Clock clock){this.npcs=npcs;this.events=events;this.clock=clock;}
 @EventHandler public void onClick(PlayerInteractEntityEvent event){npcs.actionForEntity(event.getRightClicked().getUniqueId()).ifPresent(action->{event.setCancelled(true);events.publish(new NpcActionRequested(event.getPlayer().getUniqueId(),event.getRightClicked().getUniqueId().toString(),action,clock.instant()));});}}
