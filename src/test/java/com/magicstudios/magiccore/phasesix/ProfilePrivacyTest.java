package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.afk.ShardService;
import com.magicstudios.magiccore.modules.economy.EconomyService;
import com.magicstudios.magiccore.modules.profiles.*;
import com.magicstudios.magiccore.modules.settings.*;
import com.magicstudios.magiccore.modules.statistics.PlayerStatsService;
import com.magicstudios.magiccore.ranks.RankService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePrivacyTest {
    @Test
    void privateProfileFailsClosedBeforeLoadingSensitiveData() {
        UUID viewer = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID target = UUID.fromString("00000000-0000-0000-0000-000000000011");
        PlayerSettings privateSettings = new PlayerSettings(target, Map.of(PlayerSetting.PROFILE_PUBLIC, false), Instant.EPOCH);
        PlayerSettingsService settings = new PlayerSettingsService() {
            public CompletionStage<PlayerSettings> get(UUID id){return CompletableFuture.completedFuture(privateSettings);}
            public CompletionStage<PlayerSettings> set(UUID id,PlayerSetting setting,boolean value,String key){throw new AssertionError("not used");}
        };
        CapabilityService capabilities = new CapabilityService() {
            public CompletionStage<Boolean> has(UUID id,String capability){return CompletableFuture.completedFuture(false);}
            public CompletionStage<Integer> limit(UUID id,String limit){throw new AssertionError("not used");}
            public CompletionStage<Boolean> canTarget(UUID actor,UUID targetId){throw new AssertionError("not used");}
        };
        DefaultProfileViewService service = new DefaultProfileViewService(unused(PlayerProfileService.class), settings,
                unused(PlayerStatsService.class), unused(RankService.class), unused(ShardService.class), capabilities,
                unused(AuditService.class), unused(EconomyService.class));

        ProfileView view = service.view(viewer, target).toCompletableFuture().join();
        assertThat(view.visible()).isFalse();
        assertThat(view.denialReason()).isEqualTo("PROFILE_PRIVATE");
        assertThat(service.administrativeView(viewer, target).toCompletableFuture().join().visible()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static <T> T unused(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> { throw new AssertionError("Sensitive service was called: " + method.getName()); });
    }
}
