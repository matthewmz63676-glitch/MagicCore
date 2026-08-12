package com.magicstudios.magiccore.admin;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeInputSessionService implements InputSessionService {
    private final ConcurrentHashMap<UUID, InputSession> sessions = new ConcurrentHashMap<>();
    private final Clock clock;

    public NativeInputSessionService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public InputSession begin(UUID playerId, String field, Duration timeout) {
        InputSession session = new InputSession(UUID.randomUUID(), playerId, field, clock.instant().plus(timeout), null);
        sessions.put(playerId, session);
        return session;
    }

    @Override
    public Optional<InputSession> active(UUID playerId) {
        InputSession session = sessions.get(playerId);
        if (session != null && !clock.instant().isBefore(session.expiresAt())) {
            sessions.remove(playerId, session);
            return Optional.empty();
        }
        return Optional.ofNullable(session);
    }

    @Override
    public boolean cancel(UUID playerId) {
        return sessions.remove(playerId) != null;
    }

    @Override
    public Optional<InputSession> submit(UUID playerId, String input) {
        Optional<InputSession> active = active(playerId);
        if (active.isEmpty()) return Optional.empty();
        InputSession completed = new InputSession(active.get().id(), playerId, active.get().field(), active.get().expiresAt(), input);
        sessions.remove(playerId, active.get());
        return Optional.of(completed);
    }
}
