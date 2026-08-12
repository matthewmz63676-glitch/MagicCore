package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.afk.AfkEligibilitySnapshot;
import org.bukkit.Location;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AfkPresenceTracker {
    private final Map<UUID,Session>sessions=new ConcurrentHashMap<>();
    AfkEligibilitySnapshot sample(UUID playerId,String zoneId,Location location,Instant now){Session session=sessions.compute(playerId,(ignored,current)->current==null||!current.zoneId.equals(zoneId)?new Session(zoneId,now):current);return session.sample(location,now);}
    void outside(UUID playerId){sessions.remove(playerId);}void remove(UUID playerId){sessions.remove(playerId);}
    private static final class Session{private final String zoneId;private final Instant enteredAt;private final Set<String>positions=new HashSet<>();private final Map<String,Integer>movementSignatures=new HashMap<>();private int samples,lookChanges,nonIdleTransitions,highestSignatureCount;private Location previous;
        private Session(String zoneId,Instant enteredAt){this.zoneId=zoneId;this.enteredAt=enteredAt;}
        private synchronized AfkEligibilitySnapshot sample(Location location,Instant now){samples++;positions.add(location.getBlockX()+":"+location.getBlockY()+":"+location.getBlockZ());if(previous!=null){float yawDelta=Math.abs(normalize(location.getYaw()-previous.getYaw())),pitchDelta=Math.abs(location.getPitch()-previous.getPitch());if(yawDelta>=5||pitchDelta>=5)lookChanges++;int dx=location.getBlockX()-previous.getBlockX(),dy=location.getBlockY()-previous.getBlockY(),dz=location.getBlockZ()-previous.getBlockZ(),look=Math.round(yawDelta/15F);if(dx!=0||dy!=0||dz!=0||look!=0){nonIdleTransitions++;String signature=dx+":"+dy+":"+dz+":"+look;int count=movementSignatures.merge(signature,1,Integer::sum);highestSignatureCount=Math.max(highestSignatureCount,count);}}previous=location.clone();int macroRisk=nonIdleTransitions<4?0:(int)Math.min(10_000L,(long)highestSignatureCount*10_000L/nonIdleTransitions);long sessionSeconds=Math.max(0,Duration.between(enteredAt,now).toSeconds());return new AfkEligibilitySnapshot(zoneId,sessionSeconds,sessionSeconds,samples,positions.size(),lookChanges,macroRisk);}
        private static float normalize(float degrees){float value=degrees%360F;if(value>180F)value-=360F;if(value<-180F)value+=360F;return value;}
    }
}
