package com.magicstudios.magiccore.modules.keyall;

import java.time.Instant;

public record KeyallThreshold(String definitionId, long progress, Instant updatedAt) { }
