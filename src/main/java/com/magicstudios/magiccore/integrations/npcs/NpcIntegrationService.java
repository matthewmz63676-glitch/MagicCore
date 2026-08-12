package com.magicstudios.magiccore.integrations.npcs;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface NpcIntegrationService {String provider();boolean available();CompletionStage<Boolean>upsert(NpcSpec spec);Optional<NpcAction>actionForEntity(UUID entityId);}
