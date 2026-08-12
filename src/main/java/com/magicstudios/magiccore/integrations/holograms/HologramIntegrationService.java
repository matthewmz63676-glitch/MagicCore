package com.magicstudios.magiccore.integrations.holograms;

import java.util.concurrent.CompletionStage;

public interface HologramIntegrationService {
    String provider();boolean available();CompletionStage<Boolean>upsert(HologramSpec spec);CompletionStage<Boolean>remove(String id);
}
