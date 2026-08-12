package com.magicstudios.magiccore.modules.profiles;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface ProfileViewService {
    CompletionStage<ProfileView> view(UUID viewerId, UUID targetId);
    CompletionStage<ProfileView> administrativeView(UUID viewerId, UUID targetId);
}
