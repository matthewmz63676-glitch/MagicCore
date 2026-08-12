package com.magicstudios.magiccore.integrations.holograms;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import java.util.List;

public record HologramSpec(String id, Location location, List<Component> lines) {
    public HologramSpec { location=location.clone();lines=List.copyOf(lines); }
}
