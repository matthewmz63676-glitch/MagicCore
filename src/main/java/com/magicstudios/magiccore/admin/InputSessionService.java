package com.magicstudios.magiccore.admin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface InputSessionService {
    InputSession begin(UUID playerId, String field, Duration timeout);

    Optional<InputSession> active(UUID playerId);

    boolean cancel(UUID playerId);

    Optional<InputSession> submit(UUID playerId, String input);
}
