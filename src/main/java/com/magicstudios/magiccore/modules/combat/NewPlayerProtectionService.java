package com.magicstudios.magiccore.modules.combat;
import com.magicstudios.magiccore.modules.profiles.PlayerProfile;
import com.magicstudios.magiccore.modules.profiles.PlayerProfileService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
public final class NewPlayerProtectionService{
 public enum State{UNKNOWN,PROTECTED,UNPROTECTED}
 private final ConcurrentHashMap<UUID,Instant>until=new ConcurrentHashMap<>();private final java.util.Set<UUID>loaded=ConcurrentHashMap.newKeySet();
 private final PlayerProfileService profiles;private final Clock clock;private final Duration duration;
 public NewPlayerProtectionService(PlayerProfileService profiles,Clock clock,Duration duration){this.profiles=profiles;this.clock=clock;this.duration=duration;}
 public CompletionStage<State> refresh(UUID id){return profiles.find(id).thenApply(profile->{if(profile.isEmpty()){loaded.remove(id);return State.UNKNOWN;}PlayerProfile value=profile.get();loaded.add(id);
   if(Boolean.parseBoolean(value.settings().getOrDefault("pvp.protection.removed","false")))until.remove(id);else until.put(id,value.firstSeen().plus(duration));return state(id);});}
 public State state(UUID id){if(!loaded.contains(id))return State.UNKNOWN;Instant value=until.get(id);return value!=null&&value.isAfter(clock.instant())?State.PROTECTED:State.UNPROTECTED;}
 public CompletionStage<Void> remove(UUID id,String operationKey){loaded.add(id);until.remove(id);return profiles.setSetting(id,"pvp.protection.removed","true",operationKey).thenApply(ignored->null);}
 public void invalidate(UUID id){loaded.remove(id);until.remove(id);}
}
