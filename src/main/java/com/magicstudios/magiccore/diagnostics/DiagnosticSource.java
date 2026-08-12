package com.magicstudios.magiccore.diagnostics;

import com.magicstudios.magiccore.api.HealthReport;

@FunctionalInterface
public interface DiagnosticSource {
    HealthReport inspect();
}
