package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.config.ConfigSnapshot;
import com.magicstudios.magiccore.config.model.FeaturesFile;

import java.util.concurrent.CompletionStage;

public interface AdminEditingService {
    ConfigSnapshot<FeaturesFile> featuresSnapshot();

    FeaturesFile previewFeatureMode(String featureId, ProviderMode mode, long expectedRevision);

    CompletionStage<AdminCommitResult<FeaturesFile>> setFeatureMode(AdminActor actor, String featureId,
                                                                   ProviderMode mode, long expectedRevision,
                                                                   String operationKey);
}
