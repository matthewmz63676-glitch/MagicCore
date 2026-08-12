package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.keyall.KeyallDefinition;
import com.magicstudios.magiccore.modules.keyall.KeyallRun;
import com.magicstudios.magiccore.modules.keyall.KeyallService;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import com.magicstudios.magiccore.storage.TransactionalDataStore;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KeyallController implements AutoCloseable {
    private final Plugin plugin;
    private final SchedulerFacade scheduler;
    private final TransactionalDataStore store;
    private final KeyallService service;
    private final Map<String,TaskHandle> scheduled = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public KeyallController(Plugin plugin, SchedulerFacade scheduler, TransactionalDataStore store, KeyallService service) {
        this.plugin = plugin; this.scheduler = scheduler; this.store = store; this.service = service;
    }

    public void startSchedules() {
        service.definitions().values().stream().filter(value -> !value.scheduleInterval().isZero()).forEach(this::scheduleNext);
    }

    public CompletionStage<KeyallRun> preview(String definitionId, KeyallRun.Trigger trigger) {
        KeyallDefinition definition = require(definitionId);
        return audience(definition).thenCompose(recipients -> service.preview(definitionId, trigger, recipients));
    }

    public CompletionStage<KeyallRun> runManual(String definitionId, String operationKey) {
        return preview(definitionId, KeyallRun.Trigger.MANUAL).thenCompose(run -> service.execute(run.id(), operationKey));
    }

    public CompletionStage<java.util.Optional<KeyallRun>> contribute(String definitionId, long amount, String operationKey) {
        KeyallDefinition definition = require(definitionId);
        return audience(definition).thenCompose(recipients -> service.contribute(definitionId, amount, recipients, operationKey));
    }

    public CompletionStage<Collection<UUID>> audience(KeyallDefinition definition) {
        LinkedHashSet<UUID> online = new LinkedHashSet<>();
        plugin.getServer().getOnlinePlayers().forEach(player -> online.add(player.getUniqueId()));
        if (definition.audience() == KeyallDefinition.Audience.ONLINE || !definition.offlineDelivery())
            return CompletableFuture.completedFuture(java.util.List.copyOf(online));
        return store.read(reader -> {
            LinkedHashSet<UUID> recipients = new LinkedHashSet<>(online); String after = null;
            while (true) { var page = reader.scan("profiles.player", after, 1000);
                for (var record : page) try { recipients.add(UUID.fromString(record.key())); } catch (IllegalArgumentException ignored) { }
                if (page.size() < 1000) break; after = page.getLast().key(); }
            return java.util.List.copyOf(recipients);
        });
    }

    private void scheduleNext(KeyallDefinition definition) {
        if (closed.get()) return;
        TaskHandle handle = scheduler.executeGlobalLater(definition.scheduleInterval(), () -> {
            if (closed.get()) return;
            preview(definition.id(), KeyallRun.Trigger.SCHEDULE).thenCompose(run -> service.execute(run.id(), "schedule:" + run.id()))
                    .whenComplete((run, failure) -> {
                        if (failure != null) plugin.getLogger().severe("Scheduled keyall " + definition.id() + " failed: " + failure.getMessage());
                        scheduleNext(definition);
                    });
        });
        TaskHandle previous = scheduled.put(definition.id(), handle); if (previous != null) previous.cancel();
    }

    private KeyallDefinition require(String id) {
        KeyallDefinition definition = service.definitions().get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown keyall " + id); return definition;
    }

    @Override public void close() { if (!closed.compareAndSet(false, true)) return; scheduled.values().forEach(TaskHandle::cancel); scheduled.clear(); }
}
