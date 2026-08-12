package com.magicstudios.magiccore.modules;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.api.ServiceRegistry;
import com.magicstudios.magiccore.commands.CommandRegistry;
import com.magicstudios.magiccore.diagnostics.DiagnosticsRegistry;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;

import java.util.Objects;

public record ModuleContext(ServiceRegistry services, DomainEventBus events,
                            CommandRegistry commands, PlaceholderRegistry placeholders,
                            DiagnosticsRegistry diagnostics) {
    public ModuleContext {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(placeholders, "placeholders");
        Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public void cleanupOwner(String owner) {
        services.unregisterOwner(owner);
        events.unsubscribeOwner(owner);
        commands.unregisterOwner(owner);
        placeholders.unregisterOwner(owner);
        diagnostics.unregisterOwner(owner);
    }
}
