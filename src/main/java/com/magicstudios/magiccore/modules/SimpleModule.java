package com.magicstudios.magiccore.modules;

import com.magicstudios.magiccore.api.HealthReport;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SimpleModule implements MagicModule {
    private final ModuleDescriptor descriptor;
    private final Function<ModuleContext, List<String>> validator;
    private final Consumer<ModuleContext> starter;
    private final Consumer<ModuleContext> stopper;
    private final Supplier<HealthReport> health;

    public SimpleModule(ModuleDescriptor descriptor, Function<ModuleContext, List<String>> validator,
                        Consumer<ModuleContext> starter, Consumer<ModuleContext> stopper,
                        Supplier<HealthReport> health) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.starter = Objects.requireNonNull(starter, "starter");
        this.stopper = Objects.requireNonNull(stopper, "stopper");
        this.health = Objects.requireNonNull(health, "health");
    }

    @Override public ModuleDescriptor descriptor() { return descriptor; }
    @Override public List<String> validate(ModuleContext context) { return validator.apply(context); }
    @Override public void start(ModuleContext context) { starter.accept(context); }
    @Override public void stop(ModuleContext context) { stopper.accept(context); }
    @Override public HealthReport health() { return health.get(); }
}
