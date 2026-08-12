package com.magicstudios.magiccore.modules.combat;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
public interface CombatService{
 CombatTag tag(UUID attackerId,UUID victimId);
 Optional<CombatTag> activeTag(UUID playerId);
 boolean isTagged(UUID playerId);
 Duration remaining(UUID playerId);
 CombatLogoutResolution logout(UUID playerId);
 int purgeExpired();
}
