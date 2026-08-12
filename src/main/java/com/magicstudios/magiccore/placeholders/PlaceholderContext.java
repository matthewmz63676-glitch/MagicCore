package com.magicstudios.magiccore.placeholders;

import java.util.Optional;
import java.util.UUID;

public record PlaceholderContext(UUID viewerId, UUID subjectId) {
    public Optional<UUID> viewer() {
        return Optional.ofNullable(viewerId);
    }

    public Optional<UUID> subject() {
        return Optional.ofNullable(subjectId);
    }
}
