package com.magicstudios.magiccore.admin;

import java.util.Optional;
import java.util.UUID;

public interface SetupService {
    SetupPlan begin(UUID actorId, SetupPreset preset);

    SetupPlan selectStorage(UUID actorId, String provider);

    SetupPlan review(UUID actorId);

    Optional<SetupPlan> active(UUID actorId);

    boolean cancel(UUID actorId);
}
