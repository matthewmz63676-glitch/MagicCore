package com.magicstudios.magiccore.modules.presentation;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface PresentationService {
    CompletionStage<NavigationView> info(UUID playerId);
    CompletionStage<NavigationView> serverNavigation(UUID playerId);
    CompletionStage<ApplicationView> application(UUID playerId, ApplicationKind kind);
}
