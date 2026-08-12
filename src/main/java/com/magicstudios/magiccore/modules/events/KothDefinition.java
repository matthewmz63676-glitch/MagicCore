package com.magicstudios.magiccore.modules.events;

import com.magicstudios.magiccore.config.model.EventsFile;
import java.time.Duration;
import java.util.Set;

public record KothDefinition(String id,String displayName,String world,double minimumX,double minimumY,double minimumZ,
                             double maximumX,double maximumY,double maximumZ,Duration captureTime,Duration firstDelay,
                             Duration scheduleInterval,Set<String>bannedMaterials,EventsFile.EventReward reward) {
    public KothDefinition { bannedMaterials=Set.copyOf(bannedMaterials); }
    public boolean contains(String worldId,double x,double y,double z){return world.equals(worldId)&&x>=minimumX&&x<=maximumX&&y>=minimumY&&y<=maximumY&&z>=minimumZ&&z<=maximumZ;}
}
