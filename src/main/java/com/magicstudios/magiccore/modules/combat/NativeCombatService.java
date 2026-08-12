package com.magicstudios.magiccore.modules.combat;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class NativeCombatService implements CombatService{
 private final ConcurrentHashMap<UUID,CombatTag>tags=new ConcurrentHashMap<>();private final Clock clock;private final Duration duration;
 public NativeCombatService(Clock clock,Duration duration){if(duration.isNegative()||duration.isZero())throw new IllegalArgumentException("tag duration must be positive");this.clock=clock;this.duration=duration;}
 @Override public CombatTag tag(UUID attacker,UUID victim){if(attacker.equals(victim))throw new IllegalArgumentException("self combat");var now=clock.instant();UUID tagId=UUID.randomUUID();
  CombatTag attackerTag=new CombatTag(tagId,attacker,victim,now,now.plus(duration));CombatTag victimTag=new CombatTag(tagId,victim,attacker,now,now.plus(duration));tags.put(attacker,attackerTag);tags.put(victim,victimTag);return victimTag;}
 @Override public Optional<CombatTag> activeTag(UUID player){CombatTag tag=tags.get(player);if(tag==null)return Optional.empty();if(!tag.activeAt(clock.instant())){tags.remove(player,tag);return Optional.empty();}return Optional.of(tag);}
 @Override public boolean isTagged(UUID player){return activeTag(player).isPresent();}
 @Override public Duration remaining(UUID player){return activeTag(player).map(tag->Duration.between(clock.instant(),tag.expiresAt())).orElse(Duration.ZERO);}
 @Override public CombatLogoutResolution logout(UUID player){var tag=activeTag(player);if(tag.isEmpty())return new CombatLogoutResolution(false,"NOT_TAGGED",null);CombatTag current=tag.get();tags.remove(player,current);
  CombatTag opponent=tags.get(current.opponentId());if(opponent!=null&&opponent.tagId().equals(current.tagId()))tags.remove(current.opponentId(),opponent);
  UUID eventId=UUID.randomUUID();return new CombatLogoutResolution(true,"LOGOUT_KILL",new VerifiedPlayerKill(eventId,current.opponentId(),player,"COMBAT_LOGOUT",clock.instant()));}
 @Override public int purgeExpired(){int before=tags.size();tags.entrySet().removeIf(entry->!entry.getValue().activeAt(clock.instant()));return before-tags.size();}
}
