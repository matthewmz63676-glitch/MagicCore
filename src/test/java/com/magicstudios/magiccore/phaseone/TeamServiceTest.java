package com.magicstudios.magiccore.phaseone;

import com.magicstudios.magiccore.admin.NativeInputSessionService;
import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.modules.teams.PersistentTeamService;
import com.magicstudios.magiccore.modules.teams.TeamNamePolicy;
import com.magicstudios.magiccore.modules.teams.TeamRole;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamServiceTest {
    @Test
    void oneCanonicalPolicyAcceptsSixteenCharactersAndRejectsSeventeen() {
        TeamNamePolicy policy = new TeamNamePolicy(3, 16, "[A-Za-z0-9_]+", Set.of("admin"));
        assertThat(policy.validate("1234567890ABCDEF")).isEqualTo("1234567890ABCDEF");
        assertThatThrownBy(() -> policy.validate("1234567890ABCDEFG")).hasMessageContaining("3..16");
        assertThatThrownBy(() -> policy.validate("ADMIN")).hasMessageContaining("reserved");
    }

    @Test
    void teamLifecycleInvitesRolesAndMembershipAreAtomic() {
        var store = new InMemoryTransactionalDataStore(new BoundedIoExecutor(3, 128, "teams-test"));
        try {
            CapabilityService limits = fixedTeamLimit(5);
            var service = new PersistentTeamService(store, limits, new DomainEventBus(),
                    new TeamNamePolicy(3, 16, "[A-Za-z0-9_]+", Set.of("admin")),
                    Duration.ofMinutes(5), Clock.systemUTC());
            UUID owner = UUID.randomUUID();
            UUID member = UUID.randomUUID();

            var created = service.create(owner, "Builders", "create-1").toCompletableFuture().join();
            assertThat(created.applied()).isTrue();
            assertThat(service.create(owner, "Builders", "create-1").toCompletableFuture().join().applied()).isFalse();
            service.invite(owner, member, "invite-1").toCompletableFuture().join();
            var joined = service.accept(member, created.team().id(), "accept-1").toCompletableFuture().join();
            assertThat(joined.team().members()).containsEntry(member, TeamRole.MEMBER);
            var promoted = service.setRole(owner, member, TeamRole.OFFICER, "promote-1").toCompletableFuture().join();
            assertThat(promoted.team().members()).containsEntry(member, TeamRole.OFFICER);
            service.leave(member, "leave-1").toCompletableFuture().join();
            assertThat(service.teamOf(member).toCompletableFuture().join()).isEmpty();
        } finally {
            store.close();
        }
    }

    @Test
    void nativeInputSessionCanBeCancelledWithoutSkriptAddon() {
        var sessions = new NativeInputSessionService(Clock.systemUTC());
        UUID player = UUID.randomUUID();
        sessions.begin(player, "team.name", Duration.ofMinutes(1));
        assertThat(sessions.active(player)).isPresent();
        assertThat(sessions.cancel(player)).isTrue();
        assertThat(sessions.submit(player, "Ignored")).isEmpty();
    }

    private static CapabilityService fixedTeamLimit(int limit) {
        return new CapabilityService() {
            @Override public java.util.concurrent.CompletionStage<Boolean> has(UUID playerId, String capability) {
                return CompletableFuture.completedFuture(true);
            }
            @Override public java.util.concurrent.CompletionStage<Integer> limit(UUID playerId, String limitId) {
                return CompletableFuture.completedFuture(limit);
            }
            @Override public java.util.concurrent.CompletionStage<Boolean> canTarget(UUID actorId, UUID targetId) {
                return CompletableFuture.completedFuture(true);
            }
        };
    }
}
