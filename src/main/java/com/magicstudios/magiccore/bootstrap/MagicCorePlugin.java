package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.api.ServiceRegistry;
import com.magicstudios.magiccore.commands.CommandRegistry;
import com.magicstudios.magiccore.commands.CommandSpec;
import com.magicstudios.magiccore.commands.MagicCoreCommand;
import com.magicstudios.magiccore.diagnostics.DiagnosticsRegistry;
import com.magicstudios.magiccore.modules.ModuleContext;
import com.magicstudios.magiccore.modules.ModuleManager;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.platform.PaperFoliaScheduler;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

public final class MagicCorePlugin extends JavaPlugin {
    private SchedulerFacade scheduler;
    private final AtomicReference<MagicCoreRuntime> runtimeReference = new AtomicReference<>();

    @Override
    public void onEnable() {
        BoundedIoExecutor ioExecutor = new BoundedIoExecutor(4, 1024, "magiccore-io");
        scheduler = new PaperFoliaScheduler(this, ioExecutor);
        ServiceRegistry services = new ServiceRegistry();
        DomainEventBus events = new DomainEventBus();
        CommandRegistry commands = new CommandRegistry();
        PlaceholderRegistry placeholders = new PlaceholderRegistry();
        DiagnosticsRegistry diagnostics = new DiagnosticsRegistry();
        services.register("foundation", SchedulerFacade.class, scheduler);
        commands.register(new CommandSpec("foundation", "magic", Set.of("magiccore"), "VIEW_DIAGNOSTICS"));
        MagicCoreCommand command = new MagicCoreCommand(runtimeReference, scheduler);
        java.util.Objects.requireNonNull(getCommand("magic"), "plugin.yml magic command").setExecutor(command);
        java.util.Objects.requireNonNull(getCommand("spawnstash"), "plugin.yml spawnstash command").setExecutor(command);
        getCommand("magic").setTabCompleter(command);
        MagicCoreRuntime runtime = new MagicCoreRuntime(this, scheduler, services, events, commands,
                placeholders, diagnostics, Clock.systemUTC());
        runtimeReference.set(runtime);
        runtime.start().whenComplete((ignored, failure) -> {
            if (failure == null) getLogger().info("MagicCore runtime is healthy.");
            else getLogger().severe("MagicCore startup failed: " + failure.getMessage());
        });
    }

    @Override
    public void onDisable() {
        MagicCoreRuntime runtime = runtimeReference.getAndSet(null);
        if (runtime != null) runtime.close();
        if (scheduler != null) {
            scheduler.close();
        }
    }
}
