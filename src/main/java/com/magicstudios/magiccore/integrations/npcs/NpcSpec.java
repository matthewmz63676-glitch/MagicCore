package com.magicstudios.magiccore.integrations.npcs;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public record NpcSpec(String id, EntityType entityType, String displayName, Location location, NpcAction action) {
    public NpcSpec { location=location.clone(); }
}
