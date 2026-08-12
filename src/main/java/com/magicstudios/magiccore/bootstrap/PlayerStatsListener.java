package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStatsListener implements Listener, AutoCloseable {
    private final PlayerStatsService stats; private final Clock clock; private final Map<UUID, Instant> sessions=new ConcurrentHashMap<>();
    public PlayerStatsListener(PlayerStatsService stats,Clock clock){this.stats=stats;this.clock=clock;}
    @EventHandler public void onJoin(PlayerJoinEvent event){sessions.put(event.getPlayer().getUniqueId(),clock.instant());}
    @EventHandler public void onQuit(PlayerQuitEvent event){finish(event.getPlayer().getUniqueId());}
    private void finish(UUID playerId){Instant start=sessions.remove(playerId);if(start==null)return;long seconds=Math.max(0,Duration.between(start,clock.instant()).toSeconds());
        stats.addPlaytime(playerId,seconds,"session:"+playerId+":"+start.toEpochMilli());}
    @Override public void close(){java.util.ArrayList<UUID>players=new java.util.ArrayList<>(sessions.keySet());players.forEach(this::finish);}
}
