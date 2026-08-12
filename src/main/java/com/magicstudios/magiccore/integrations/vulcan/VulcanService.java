package com.magicstudios.magiccore.integrations.vulcan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VulcanService extends AutoCloseable {
    boolean available();
    String status();
    List<VulcanFlag> recentFlags(UUID playerId,Instant after);
    @Override void close();
}
