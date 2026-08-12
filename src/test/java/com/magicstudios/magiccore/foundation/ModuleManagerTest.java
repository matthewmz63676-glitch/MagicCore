package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.api.ProviderMode;
import com.magicstudios.magiccore.api.ServiceRegistry;
import com.magicstudios.magiccore.commands.CommandRegistry;
import com.magicstudios.magiccore.commands.CommandSpec;
import com.magicstudios.magiccore.diagnostics.DiagnosticsRegistry;
import com.magicstudios.magiccore.modules.MagicModule;
import com.magicstudios.magiccore.modules.ModuleContext;
import com.magicstudios.magiccore.modules.ModuleDescriptor;
import com.magicstudios.magiccore.modules.ModuleException;
import com.magicstudios.magiccore.modules.ModuleManager;
import com.magicstudios.magiccore.modules.ModuleState;
import com.magicstudios.magiccore.placeholders.PlaceholderRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleManagerTest {
    @Test
    void startsDependenciesBeforeDependantsAndCleansOnStop() {
        Fixture fixture = new Fixture();
        List<String> events = new ArrayList<>();
        MagicModule profiles = fixture.module("profiles", ProviderMode.INTERNAL, Set.of(), events);
        MagicModule economy = fixture.module("economy", ProviderMode.INTERNAL, Set.of("profiles"), events);
        fixture.manager.discover(List.of(economy, profiles));

        fixture.manager.startAll();

        assertThat(events).containsExactly("start:profiles", "start:economy");
        assertThat(fixture.manager.states()).containsEntry("profiles", ModuleState.HEALTHY)
                .containsEntry("economy", ModuleState.HEALTHY);

        fixture.manager.stopAll();
        assertThat(events).containsExactly("start:profiles", "start:economy", "stop:economy", "stop:profiles");
        assertThat(fixture.services.snapshot()).isEmpty();
    }

    @Test
    void disabledModuleRegistersNothing() {
        Fixture fixture = new Fixture();
        MagicModule disabled = fixture.module("teams", ProviderMode.DISABLED, Set.of(), new ArrayList<>());
        fixture.manager.discover(List.of(disabled));

        fixture.manager.startAll();

        assertThat(fixture.manager.states()).containsEntry("teams", ModuleState.DISABLED);
        assertThat(fixture.services.snapshot()).isEmpty();
        assertThat(fixture.commands.snapshot()).isEmpty();
        assertThat(fixture.placeholders.owners()).isEmpty();
    }

    @Test
    void rejectsMissingAndCircularDependenciesBeforeEnable() {
        Fixture missing = new Fixture();
        missing.manager.discover(List.of(missing.module("economy", ProviderMode.INTERNAL, Set.of("profiles"), new ArrayList<>())));
        assertThatThrownBy(missing.manager::startAll).isInstanceOf(ModuleException.class)
                .hasMessageContaining("requires missing module profiles");

        Fixture circular = new Fixture();
        circular.manager.discover(List.of(
                circular.module("profiles", ProviderMode.INTERNAL, Set.of("economy"), new ArrayList<>()),
                circular.module("economy", ProviderMode.INTERNAL, Set.of("profiles"), new ArrayList<>())));
        assertThatThrownBy(circular.manager::startAll).isInstanceOf(ModuleException.class)
                .hasMessageContaining("Circular module dependency");
    }

    private static final class Fixture {
        private final ServiceRegistry services = new ServiceRegistry();
        private final CommandRegistry commands = new CommandRegistry();
        private final PlaceholderRegistry placeholders = new PlaceholderRegistry();
        private final ModuleContext context = new ModuleContext(services, new DomainEventBus(), commands,
                placeholders, new DiagnosticsRegistry());
        private final ModuleManager manager = new ModuleManager(context);

        private MagicModule module(String id, ProviderMode mode, Set<String> dependencies, List<String> events) {
            return new MagicModule() {
                @Override
                public ModuleDescriptor descriptor() {
                    return new ModuleDescriptor(id, mode, dependencies);
                }

                @Override
                public void start(ModuleContext context) {
                    events.add("start:" + id);
                    if (id.equals("profiles")) {
                        context.services().register(id, Marker.class, new Marker() { });
                    }
                    context.commands().register(new CommandSpec(id, id, Set.of(), "TEST"));
                    context.placeholders().register(id, id + "_value", ignored -> "ok");
                }

                @Override
                public void stop(ModuleContext context) {
                    events.add("stop:" + id);
                }
            };
        }
    }

    private interface Marker {
    }
}
