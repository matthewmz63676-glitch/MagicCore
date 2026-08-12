package com.magicstudios.magiccore.modules.store;

import java.time.Instant;

public record DonationGoalState(long contributedMinor, long targetMinor, Instant updatedAt) {
    public double progress(){return targetMinor<=0?0:Math.min(1.0,(double)contributedMinor/targetMinor);}
}
