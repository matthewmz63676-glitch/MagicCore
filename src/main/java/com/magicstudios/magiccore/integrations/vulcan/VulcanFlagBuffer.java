package com.magicstudios.magiccore.integrations.vulcan;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VulcanFlagBuffer {
    private final Duration retention;private final int maximumPerPlayer;private final Map<UUID,ArrayDeque<VulcanFlag>>flags=new ConcurrentHashMap<>();
    public VulcanFlagBuffer(Duration retention,int maximumPerPlayer){if(retention.isNegative()||retention.isZero()||maximumPerPlayer<1||maximumPerPlayer>10_000)throw new IllegalArgumentException("Invalid Vulcan flag buffer policy");this.retention=retention;this.maximumPerPlayer=maximumPerPlayer;}
    public void record(VulcanFlag flag){ArrayDeque<VulcanFlag>queue=flags.computeIfAbsent(flag.playerId(),ignored->new ArrayDeque<>());synchronized(queue){prune(queue,flag.observedAt());queue.addLast(flag);while(queue.size()>maximumPerPlayer)queue.removeFirst();}}
    public List<VulcanFlag>recent(UUID playerId,Instant after){ArrayDeque<VulcanFlag>queue=flags.get(playerId);if(queue==null)return List.of();synchronized(queue){prune(queue,Instant.now());return queue.stream().filter(flag->!flag.observedAt().isBefore(after)).toList();}}
    public int trackedPlayers(){return flags.size();}
    private void prune(ArrayDeque<VulcanFlag>queue,Instant now){Instant cutoff=now.minus(retention);while(!queue.isEmpty()&&queue.getFirst().observedAt().isBefore(cutoff))queue.removeFirst();}
}
