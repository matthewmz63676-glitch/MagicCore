package com.magicstudios.magiccore.modules.combat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
public final class AbilityCooldownService{
 public enum Ability{ENDER_PEARL,TRIDENT}
 private final ConcurrentHashMap<String,Instant>until=new ConcurrentHashMap<>();private final Clock clock;
 public AbilityCooldownService(Clock clock){this.clock=clock;}
 public boolean tryUse(UUID player,Ability ability,Duration cooldown){if(cooldown.isNegative())throw new IllegalArgumentException("cooldown must not be negative");String key=player+":"+ability;Instant now=clock.instant();AtomicBoolean accepted=new AtomicBoolean();
  until.compute(key,(ignored,current)->{if(current==null||!current.isAfter(now)){accepted.set(true);return now.plus(cooldown);}return current;});return accepted.get();}
 public Duration remaining(UUID player,Ability ability){Instant value=until.get(player+":"+ability);return value==null||!value.isAfter(clock.instant())?Duration.ZERO:Duration.between(clock.instant(),value);}
}
