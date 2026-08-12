package com.magicstudios.magiccore.modules.teams;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentTeamService implements TeamService {
    private static final String MEMBER_INDEX = "teams.member-index";
    private static final String NAME_INDEX = "teams.name-index";
    private final TransactionalDataStore store;
    private final CapabilityService capabilities;
    private final DomainEventBus events;
    private final TeamNamePolicy namePolicy;
    private final Duration invitationTtl;
    private final Clock clock;
    private final RecordRepository<Team> teams = new RecordRepository<>("teams.team", Team.class);
    private final RecordRepository<TeamInvitation> invitations = new RecordRepository<>("teams.invitation", TeamInvitation.class);

    public PersistentTeamService(TransactionalDataStore store, CapabilityService capabilities, DomainEventBus events,
                                 TeamNamePolicy namePolicy, Duration invitationTtl, Clock clock) {
        this.store = store;
        this.capabilities = capabilities;
        this.events = events;
        this.namePolicy = namePolicy;
        this.invitationTtl = invitationTtl;
        this.clock = clock;
    }

    @Override
    public TeamNamePolicy namePolicy() {
        return namePolicy;
    }

    @Override
    public CompletionStage<Optional<Team>> teamOf(UUID playerId) {
        return store.read(reader -> {
            var index = reader.get(MEMBER_INDEX, playerId.toString());
            if (index.isEmpty()) return Optional.empty();
            String teamId = new String(index.get().payload(), StandardCharsets.UTF_8);
            return teams.get(reader, teamId).map(RecordRepository.VersionedValue::value);
        });
    }

    @Override
    public CompletionStage<TeamMutation> create(UUID ownerId, String name, String operationKey) {
        String validName = namePolicy.validate(name);
        return store.transact("team-create:" + operationKey, transaction -> {
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return replay(transaction, ownerId);
            if (transaction.get(MEMBER_INDEX, ownerId.toString()).isPresent()) throw new IllegalStateException("ALREADY_IN_TEAM");
            String normalized = namePolicy.normalize(validName);
            if (transaction.get(NAME_INDEX, normalized).isPresent()) throw new IllegalStateException("TEAM_NAME_TAKEN");
            UUID id = UUID.randomUUID();
            Team team = new Team(id, validName, normalized, ownerId, Map.of(ownerId, TeamRole.LEADER), clock.instant(), 1);
            teams.put(transaction, id.toString(), team, 0);
            transaction.put(NAME_INDEX, normalized, bytes(id), 0);
            transaction.put(MEMBER_INDEX, ownerId.toString(), bytes(id), 0);
            return new TeamMutation(true, team, "CREATED");
        }).thenApply(result -> publish(result, ownerId, "CREATED"));
    }

    @Override
    public CompletionStage<TeamMutation> rename(UUID actorId, String name, String operationKey) {
        String validName = namePolicy.validate(name);
        return mutateOwned(actorId, operationKey, "RENAMED", (transaction, current) -> {
            String normalized = namePolicy.normalize(validName);
            if (transaction.get(NAME_INDEX, normalized).isPresent() && !normalized.equals(current.value().normalizedName())) {
                throw new IllegalStateException("TEAM_NAME_TAKEN");
            }
            transaction.delete(NAME_INDEX, current.value().normalizedName(),
                    transaction.get(NAME_INDEX, current.value().normalizedName()).orElseThrow().revision());
            transaction.put(NAME_INDEX, normalized, bytes(current.value().id()), 0);
            Team team = new Team(current.value().id(), validName, normalized, current.value().ownerId(),
                    current.value().members(), current.value().createdAt(), current.value().revision() + 1);
            teams.put(transaction, team.id().toString(), team, current.revision());
            return team;
        });
    }

    @Override
    public CompletionStage<TeamMutation> disband(UUID actorId, String operationKey) {
        return store.transact("team-disband:" + operationKey, transaction -> {
            var current = requireTeam(transaction, actorId);
            requireLeader(current.value(), actorId);
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false, current.value(), "REPLAY");
            for (UUID member : current.value().members().keySet()) {
                transaction.get(MEMBER_INDEX, member.toString()).ifPresent(record -> {
                    try { transaction.delete(MEMBER_INDEX, member.toString(), record.revision()); }
                    catch (Exception failure) { throw new IllegalStateException(failure); }
                });
            }
            transaction.delete(NAME_INDEX, current.value().normalizedName(),
                    transaction.get(NAME_INDEX, current.value().normalizedName()).orElseThrow().revision());
            transaction.delete("teams.team", current.value().id().toString(), current.revision());
            return new TeamMutation(true, current.value(), "DISBANDED");
        }).thenApply(result -> publish(result, actorId, "DISBANDED"));
    }

    @Override
    public CompletionStage<TeamMutation> invite(UUID actorId, UUID targetId, String operationKey) {
        return store.transact("team-invite:" + operationKey, transaction -> {
            var current = requireTeam(transaction, actorId);
            TeamRole actorRole = current.value().members().get(actorId);
            if (actorRole != TeamRole.LEADER && actorRole != TeamRole.OFFICER) throw new IllegalStateException("NOT_TEAM_MANAGER");
            if (transaction.get(MEMBER_INDEX, targetId.toString()).isPresent()) throw new IllegalStateException("TARGET_ALREADY_IN_TEAM");
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false, current.value(), "REPLAY");
            TeamInvitation invite = new TeamInvitation(current.value().id(), targetId, actorId, clock.instant(), clock.instant().plus(invitationTtl));
            var existing = invitations.get(transaction, inviteKey(current.value().id(), targetId));
            invitations.put(transaction, inviteKey(current.value().id(), targetId), invite,
                    existing.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return new TeamMutation(true, current.value(), "INVITED");
        }).thenApply(result -> publish(result, actorId, "INVITED"));
    }

    @Override
    public CompletionStage<TeamMutation> accept(UUID playerId, UUID teamId, String operationKey) {
        return store.read(reader -> teams.get(reader, teamId.toString()).orElseThrow().value().ownerId())
                .thenCompose(owner -> capabilities.limit(owner, "TEAM_SIZE"))
                .thenCompose(limit -> store.transact("team-accept:" + operationKey, transaction -> {
                    if (transaction.get(MEMBER_INDEX, playerId.toString()).isPresent()) throw new IllegalStateException("ALREADY_IN_TEAM");
                    var current = teams.get(transaction, teamId.toString()).orElseThrow(() -> new IllegalStateException("TEAM_NOT_FOUND"));
                    var invite = invitations.get(transaction, inviteKey(teamId, playerId)).orElseThrow(() -> new IllegalStateException("INVITE_NOT_FOUND"));
                    if (invite.value().expired(clock.instant())) throw new IllegalStateException("INVITE_EXPIRED");
                    if (current.value().members().size() >= limit) throw new IllegalStateException("TEAM_FULL");
                    if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false, current.value(), "REPLAY");
                    Map<UUID, TeamRole> members = new LinkedHashMap<>(current.value().members());
                    members.put(playerId, TeamRole.MEMBER);
                    Team updated = updateMembers(current.value(), members);
                    teams.put(transaction, teamId.toString(), updated, current.revision());
                    transaction.put(MEMBER_INDEX, playerId.toString(), bytes(teamId), 0);
                    transaction.delete("teams.invitation", inviteKey(teamId, playerId), invite.revision());
                    return new TeamMutation(true, updated, "JOINED");
                })).thenApply(result -> publish(result, playerId, "JOINED"));
    }

    @Override
    public CompletionStage<TeamMutation> decline(UUID playerId, UUID teamId, String operationKey) {
        return store.transact("team-decline:" + operationKey, transaction -> {
            var invite = invitations.get(transaction, inviteKey(teamId, playerId)).orElseThrow(() -> new IllegalStateException("INVITE_NOT_FOUND"));
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false,
                    teams.get(transaction, teamId.toString()).orElseThrow().value(), "REPLAY");
            transaction.delete("teams.invitation", inviteKey(teamId, playerId), invite.revision());
            return new TeamMutation(true, teams.get(transaction, teamId.toString()).orElseThrow().value(), "DECLINED");
        });
    }

    @Override
    public CompletionStage<TeamMutation> leave(UUID playerId, String operationKey) {
        return store.transact("team-leave:" + operationKey, transaction -> {
            var current = requireTeam(transaction, playerId);
            if (current.value().ownerId().equals(playerId)) throw new IllegalStateException("LEADER_MUST_DISBAND_OR_TRANSFER");
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false, current.value(), "REPLAY");
            Map<UUID, TeamRole> members = new LinkedHashMap<>(current.value().members());
            members.remove(playerId);
            Team updated = updateMembers(current.value(), members);
            teams.put(transaction, updated.id().toString(), updated, current.revision());
            transaction.delete(MEMBER_INDEX, playerId.toString(), transaction.get(MEMBER_INDEX, playerId.toString()).orElseThrow().revision());
            return new TeamMutation(true, updated, "LEFT");
        }).thenApply(result -> publish(result, playerId, "LEFT"));
    }

    @Override
    public CompletionStage<TeamMutation> kick(UUID actorId, UUID targetId, String operationKey) {
        return mutateOwned(actorId, operationKey, "KICKED", (transaction, current) -> {
            if (current.value().ownerId().equals(targetId)) throw new IllegalStateException("CANNOT_KICK_LEADER");
            if (!current.value().members().containsKey(targetId)) throw new IllegalStateException("NOT_A_MEMBER");
            Map<UUID, TeamRole> members = new LinkedHashMap<>(current.value().members());
            members.remove(targetId);
            transaction.delete(MEMBER_INDEX, targetId.toString(), transaction.get(MEMBER_INDEX, targetId.toString()).orElseThrow().revision());
            Team updated = updateMembers(current.value(), members);
            teams.put(transaction, updated.id().toString(), updated, current.revision());
            return updated;
        });
    }

    @Override
    public CompletionStage<TeamMutation> setRole(UUID actorId, UUID targetId, TeamRole role, String operationKey) {
        if (role == TeamRole.LEADER) throw new IllegalArgumentException("Leadership transfer requires a dedicated operation");
        return mutateOwned(actorId, operationKey, "ROLE_CHANGED", (transaction, current) -> {
            if (current.value().ownerId().equals(targetId)) throw new IllegalStateException("CANNOT_CHANGE_LEADER_ROLE");
            if (!current.value().members().containsKey(targetId)) throw new IllegalStateException("NOT_A_MEMBER");
            Map<UUID, TeamRole> members = new LinkedHashMap<>(current.value().members());
            members.put(targetId, role);
            Team updated = updateMembers(current.value(), members);
            teams.put(transaction, updated.id().toString(), updated, current.revision());
            return updated;
        });
    }

    private CompletionStage<TeamMutation> mutateOwned(UUID actorId, String operationKey, String code, TeamUpdate update) {
        return store.transact("team-" + code.toLowerCase() + ":" + operationKey, transaction -> {
            var current = requireTeam(transaction, actorId);
            requireLeader(current.value(), actorId);
            if (!IdempotencyKeys.reserve(transaction, "team", operationKey)) return new TeamMutation(false, current.value(), "REPLAY");
            return new TeamMutation(true, update.apply(transaction, current), code);
        }).thenApply(result -> publish(result, actorId, code));
    }

    private RecordRepository.VersionedValue<Team> requireTeam(com.magicstudios.magiccore.storage.DataReader reader, UUID member) throws Exception {
        var index = reader.get(MEMBER_INDEX, member.toString()).orElseThrow(() -> new IllegalStateException("NOT_IN_TEAM"));
        UUID teamId = UUID.fromString(new String(index.payload(), StandardCharsets.UTF_8));
        return teams.get(reader, teamId.toString()).orElseThrow(() -> new IllegalStateException("TEAM_NOT_FOUND"));
    }

    private TeamMutation replay(com.magicstudios.magiccore.storage.DataReader reader, UUID playerId) throws Exception {
        return new TeamMutation(false, requireTeam(reader, playerId).value(), "REPLAY");
    }

    private TeamMutation publish(TeamMutation mutation, UUID actor, String change) {
        if (mutation.applied()) events.publish(new TeamChanged(mutation.team().id(), change, actor, clock.instant()));
        return mutation;
    }

    private static void requireLeader(Team team, UUID actor) {
        if (!team.ownerId().equals(actor)) throw new IllegalStateException("LEADER_REQUIRED");
    }

    private Team updateMembers(Team team, Map<UUID, TeamRole> members) {
        return new Team(team.id(), team.name(), team.normalizedName(), team.ownerId(), members, team.createdAt(), team.revision() + 1);
    }

    private static byte[] bytes(UUID id) {
        return id.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String inviteKey(UUID teamId, UUID playerId) {
        return teamId + ":" + playerId;
    }

    @FunctionalInterface
    private interface TeamUpdate {
        Team apply(com.magicstudios.magiccore.storage.DataTransaction transaction,
                   RecordRepository.VersionedValue<Team> current) throws Exception;
    }
}
