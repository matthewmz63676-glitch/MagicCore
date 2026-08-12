package com.magicstudios.magiccore.modules.combat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class CombatLogoutRegistry{private final Set<UUID>victims=ConcurrentHashMap.newKeySet();public void mark(UUID id){victims.add(id);}public boolean consume(UUID id){return victims.remove(id);}public void clear(UUID id){victims.remove(id);}}
