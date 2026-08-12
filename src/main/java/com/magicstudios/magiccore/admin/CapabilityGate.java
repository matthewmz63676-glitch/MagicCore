package com.magicstudios.magiccore.admin;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface CapabilityGate {
    CompletionStage<Boolean> has(AdminActor actor, String capability);
}
