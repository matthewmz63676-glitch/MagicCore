package com.magicstudios.magiccore.modules;

import com.magicstudios.magiccore.api.HealthState;
import com.magicstudios.magiccore.api.ProviderMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ModuleManager {
    private final ModuleContext context;
    private final Map<String, MagicModule> modules = new LinkedHashMap<>();
    private final Map<String, ModuleState> states = new LinkedHashMap<>();
    private final List<String> startOrder = new ArrayList<>();

    public ModuleManager(ModuleContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public synchronized void discover(Collection<? extends MagicModule> candidates) {
        if (!startOrder.isEmpty()) {
            throw new IllegalStateException("Cannot discover modules after startup");
        }
        for (MagicModule module : candidates) {
            String id = module.descriptor().id();
            if (modules.putIfAbsent(id, module) != null) {
                throw new ModuleException("Duplicate module ID: " + id);
            }
            states.put(id, ModuleState.DISCOVERED);
        }
    }

    public synchronized void startAll() {
        List<String> order = resolveOrder();
        for (String id : order) {
            MagicModule module = modules.get(id);
            if (module.descriptor().mode() == ProviderMode.DISABLED) {
                states.put(id, ModuleState.DISABLED);
                context.cleanupOwner(id);
                continue;
            }
            List<String> errors = module.validate(context);
            if (!errors.isEmpty()) {
                states.put(id, ModuleState.FAILED);
                context.cleanupOwner(id);
                throw new ModuleException("Module " + id + " validation failed: " + String.join("; ", errors));
            }
            states.put(id, ModuleState.VALIDATED);
            states.put(id, ModuleState.STARTING);
            try {
                module.start(context);
                HealthState health = module.health().state();
                states.put(id, health == HealthState.HEALTHY ? ModuleState.HEALTHY : ModuleState.DEGRADED);
                startOrder.add(id);
            } catch (Exception failure) {
                states.put(id, ModuleState.FAILED);
                context.cleanupOwner(id);
                stopStarted();
                throw new ModuleException("Module " + id + " failed to start", failure);
            }
        }
    }

    public synchronized void stopAll() {
        stopStarted();
    }

    public synchronized Map<String, ModuleState> states() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(states));
    }

    public synchronized List<String> resolvedStartOrder() {
        return List.copyOf(startOrder);
    }

    private void stopStarted() {
        List<String> reverse = new ArrayList<>(startOrder);
        Collections.reverse(reverse);
        for (String id : reverse) {
            MagicModule module = modules.get(id);
            states.put(id, ModuleState.STOPPING);
            try {
                module.stop(context);
                states.put(id, ModuleState.DISABLED);
            } catch (Exception failure) {
                states.put(id, ModuleState.FAILED);
            } finally {
                context.cleanupOwner(id);
            }
        }
        startOrder.clear();
    }

    private List<String> resolveOrder() {
        Map<String, Visit> visits = new HashMap<>();
        Deque<String> path = new ArrayDeque<>();
        List<String> order = new ArrayList<>();
        for (String id : modules.keySet()) {
            visit(id, visits, path, order);
        }
        return order;
    }

    private void visit(String id, Map<String, Visit> visits, Deque<String> path, List<String> order) {
        Visit current = visits.get(id);
        if (current == Visit.COMPLETE) {
            return;
        }
        if (current == Visit.ACTIVE) {
            List<String> cycle = new ArrayList<>(path);
            cycle.add(id);
            throw new ModuleException("Circular module dependency: " + String.join(" -> ", cycle));
        }
        MagicModule module = modules.get(id);
        if (module == null) {
            throw new ModuleException("Missing required module: " + id);
        }
        visits.put(id, Visit.ACTIVE);
        path.addLast(id);
        Set<String> dependencies = module.descriptor().requiredModules();
        for (String dependency : dependencies.stream().sorted().toList()) {
            MagicModule dependencyModule = modules.get(dependency);
            if (dependencyModule == null) {
                throw new ModuleException("Module " + id + " requires missing module " + dependency);
            }
            if (dependencyModule.descriptor().mode() == ProviderMode.DISABLED) {
                throw new ModuleException("Module " + id + " requires disabled module " + dependency);
            }
            visit(dependency, visits, path, order);
        }
        path.removeLast();
        visits.put(id, Visit.COMPLETE);
        order.add(id);
    }

    private enum Visit { ACTIVE, COMPLETE }
}
