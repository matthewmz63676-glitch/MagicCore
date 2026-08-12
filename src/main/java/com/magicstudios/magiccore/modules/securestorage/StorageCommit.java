package com.magicstudios.magiccore.modules.securestorage;

import java.time.Instant;
import java.util.UUID;

public record StorageCommit(UUID id, UUID leaseId, UUID ownerId, VirtualContainer.Type type, int containerIndex,
                            long previousRevision, long revision, int itemCount, Instant committedAt) { }
