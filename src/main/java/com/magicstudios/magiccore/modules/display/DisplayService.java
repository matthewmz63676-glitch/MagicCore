package com.magicstudios.magiccore.modules.display;

import java.util.UUID;

public interface DisplayService {
    String provider();
    void refresh(UUID playerId);
    void refreshAll();
}
