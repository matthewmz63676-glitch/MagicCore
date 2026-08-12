package com.magicstudios.magiccore.modules.crates;

import java.util.UUID;

public record CrateOpenCount(UUID playerId, String crateId, long count) { }
