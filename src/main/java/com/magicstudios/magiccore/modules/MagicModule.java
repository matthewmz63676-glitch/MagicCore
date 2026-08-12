package com.magicstudios.magiccore.modules;

import com.magicstudios.magiccore.api.HealthReport;

import java.util.List;

public interface MagicModule {
    ModuleDescriptor descriptor();

    default List<String> validate(ModuleContext context) {
        return List.of();
    }

    void start(ModuleContext context) throws Exception;

    void stop(ModuleContext context) throws Exception;

    default HealthReport health() {
        return HealthReport.healthy(descriptor().id());
    }
}
