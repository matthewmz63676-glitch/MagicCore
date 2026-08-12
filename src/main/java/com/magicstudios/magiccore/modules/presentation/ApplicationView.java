package com.magicstudios.magiccore.modules.presentation;

import java.util.List;

public record ApplicationView(ApplicationKind kind, String title, String applyUrl,
                              boolean eligible, int satisfiedRequirements,
                              List<RequirementProgress> requirements) {
    public ApplicationView {
        requirements = List.copyOf(requirements);
    }

    public int totalRequirements() {
        return requirements.size();
    }
}
