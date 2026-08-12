package com.magicstudios.magiccore.platform;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PaperFoliaScheduler implements SchedulerFacade {
    private final Plugin plugin;
    private final Server server;
    private final BoundedIoExecutor ioExecutor;

    public PaperFoliaScheduler(Plugin plugin, BoundedIoExecutor ioExecutor) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
    }

    @Override
    public TaskHandle executeGlobal(Runnable task) {
        ScheduledTask scheduled = server.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
        return adapt(scheduled);
    }

    @Override
    public TaskHandle executeGlobalLater(Duration delay, Runnable task) {
        long ticks = Math.max(1L, (delay.toMillis() + 49L) / 50L);
        ScheduledTask scheduled = server.getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), ticks);
        return adapt(scheduled);
    }

    @Override
    public TaskHandle executeRegion(Location location, Runnable task) {
        ScheduledTask scheduled = server.getRegionScheduler().run(plugin, location, ignored -> task.run());
        return adapt(scheduled);
    }

    @Override
    public TaskHandle executeEntity(Entity entity, Runnable task, Runnable retired) {
        AtomicBoolean cancelled = new AtomicBoolean();
        entity.getScheduler().run(plugin, ignored -> task.run(), () -> {
            cancelled.set(true);
            retired.run();
        });
        return new TaskHandle() {
            @Override
            public void cancel() {
                cancelled.set(true);
            }

            @Override
            public boolean cancelled() {
                return cancelled.get();
            }
        };
    }

    @Override
    public <T> CompletionStage<T> supplyAsync(Callable<T> task) {
        return ioExecutor.submit(task);
    }

    @Override
    public void close() {
        server.getAsyncScheduler().cancelTasks(plugin);
        server.getGlobalRegionScheduler().cancelTasks(plugin);
        ioExecutor.close();
    }

    private static TaskHandle adapt(ScheduledTask scheduled) {
        return new TaskHandle() {
            @Override
            public void cancel() {
                scheduled.cancel();
            }

            @Override
            public boolean cancelled() {
                return scheduled.isCancelled();
            }
        };
    }
}
