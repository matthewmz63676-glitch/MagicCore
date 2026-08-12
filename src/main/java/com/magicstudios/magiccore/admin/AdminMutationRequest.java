package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.config.ConfigChange;

import java.util.Objects;

public record AdminMutationRequest<T>(AdminActor actor, String requiredCapability,
                                      String target, String operationKey, ConfigChange<T> change) {
    public AdminMutationRequest {
        actor = Objects.requireNonNull(actor, "actor");
        requiredCapability = Objects.requireNonNull(requiredCapability, "requiredCapability");
        target = Objects.requireNonNull(target, "target");
        operationKey = Objects.requireNonNull(operationKey, "operationKey");
        change = Objects.requireNonNull(change, "change");
    }
}
