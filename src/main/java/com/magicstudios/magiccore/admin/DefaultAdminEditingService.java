package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.config.AtomicConfigStore;
import com.magicstudios.magiccore.config.ConfigChange;
import com.magicstudios.magiccore.config.ConfigSnapshot;
import com.magicstudios.magiccore.config.model.FeaturesFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public final class DefaultAdminEditingService implements AdminEditingService {
    private final AdminConfigBackend<FeaturesFile> backend;

    public DefaultAdminEditingService(AtomicConfigStore<FeaturesFile> store,
                                      CapabilityGate capabilities,
                                      com.magicstudios.magiccore.audit.AuditService audit,
                                      java.time.Clock clock) {
        this.backend = new AdminConfigBackend<>(store, capabilities, audit, clock);
    }

    @Override
    public ConfigSnapshot<FeaturesFile> featuresSnapshot() {
        return backend.snapshot();
    }

    @Override
    public FeaturesFile previewFeatureMode(String featureId, ProviderMode mode, long expectedRevision) {
        AdminActor previewActor = AdminActor.consoleActor();
        return backend.preview(request(previewActor, featureId, mode, expectedRevision, "preview"));
    }

    @Override
    public CompletionStage<AdminCommitResult<FeaturesFile>> setFeatureMode(AdminActor actor, String featureId,
                                                                          ProviderMode mode, long expectedRevision,
                                                                          String operationKey) {
        return backend.commit(request(actor, featureId, mode, expectedRevision, operationKey));
    }

    private AdminMutationRequest<FeaturesFile> request(AdminActor actor, String featureId, ProviderMode mode,
                                                       long expectedRevision, String operationKey) {
        if (!featureId.matches("[a-z][a-z0-9-]*")) throw new IllegalArgumentException("Unknown feature ID format: " + featureId);
        ConfigChange<FeaturesFile> change = new ConfigChange<>(expectedRevision, actor.displayName(), "ADMIN_BACKEND",
                "Set " + featureId + " provider to " + mode, true, current -> {
            if (!current.features().containsKey(featureId)) throw new IllegalArgumentException("Unknown feature: " + featureId);
            Map<String, ProviderMode> updated = new LinkedHashMap<>(current.features());
            updated.put(featureId, mode);
            return new FeaturesFile(current.configVersion(), updated);
        });
        return new AdminMutationRequest<>(actor, "MANAGE_MODULES", "features.yml", operationKey, change);
    }
}
