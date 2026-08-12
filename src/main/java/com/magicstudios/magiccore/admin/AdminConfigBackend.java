package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.audit.AuditEvent;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.ConfigSnapshot;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class AdminConfigBackend<T> {
    private final AtomicConfigStore<T> configs;
    private final CapabilityGate capabilities;
    private final AuditService audit;
    private final Clock clock;

    public AdminConfigBackend(AtomicConfigStore<T> configs, CapabilityGate capabilities, AuditService audit, Clock clock) {
        this.configs = configs;
        this.capabilities = capabilities;
        this.audit = audit;
        this.clock = clock;
    }

    public ConfigSnapshot<T> snapshot() {
        return configs.snapshot();
    }

    public T preview(AdminMutationRequest<T> request) {
        ConfigSnapshot<T> current = configs.snapshot();
        if (current.revision() != request.change().expectedRevision()) {
            throw new com.magicstudios.magiccore.config.ConfigRevisionConflictException(
                    request.change().expectedRevision(), current.revision());
        }
        return request.change().mutation().apply(current.value());
    }

    public CompletionStage<AdminCommitResult<T>> commit(AdminMutationRequest<T> request) {
        // Authorization is deliberately evaluated immediately before the mutation, never at editor-open time.
        return capabilities.has(request.actor(), request.requiredCapability()).thenCompose(allowed -> {
            if (!allowed) throw new SecurityException("Capability required at commit time: " + request.requiredCapability());
            T before = configs.snapshot().value();
            return configs.commit(request.change()).thenCompose(commit -> {
                AuditEvent event = new AuditEvent(UUID.randomUUID(), request.operationKey(), "CONFIG_COMMIT",
                        request.actor().displayName(), request.target(),
                        Map.of("revision", Long.toString(commit.snapshot().revision() - 1), "value", before.toString()),
                        Map.of("revision", Long.toString(commit.snapshot().revision()), "value", commit.snapshot().value().toString()),
                        request.change().source(), clock.instant());
                return audit.record(event).thenApply(recorded -> new AdminCommitResult<>(commit, recorded));
            });
        });
    }
}
