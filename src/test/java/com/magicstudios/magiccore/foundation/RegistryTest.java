package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.api.ServiceRegistry;
import com.magicstudios.magiccore.commands.CommandConflictException;
import com.magicstudios.magiccore.commands.CommandRegistry;
import com.magicstudios.magiccore.commands.CommandSpec;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryTest {
    @Test
    void servicesHaveOneExplicitOwner() {
        ServiceRegistry registry = new ServiceRegistry();
        Runnable service = () -> { };
        registry.register("profiles", Runnable.class, service);

        assertThat(registry.require(Runnable.class)).isSameAs(service);
        assertThatThrownBy(() -> registry.register("other", Runnable.class, () -> { }))
                .hasMessageContaining("already owned by profiles");

        registry.unregisterOwner("profiles");
        assertThat(registry.find(Runnable.class)).isEmpty();
    }

    @Test
    void storeUsesStoreAliasAndNeverClaimsBuy() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new CommandSpec("store", "store", Set.of(), "STORE"));
        registry.register(new CommandSpec("shop", "shop", Set.of("buy"), "SHOP"));

        assertThat(registry.ownerOf("store")).contains("store");
        assertThat(registry.ownerOf("buy")).contains("shop");
    }

    @Test
    void aliasCollisionFailsWithActionableAlternative() {
        CommandRegistry registry = new CommandRegistry();
        registry.observeExternal("SomeShop", "buy");

        assertThatThrownBy(() -> registry.register(new CommandSpec("shop", "shop", Set.of("buy"), "SHOP")))
                .isInstanceOf(CommandConflictException.class)
                .hasMessageContaining("/magic-buy");
    }
}
