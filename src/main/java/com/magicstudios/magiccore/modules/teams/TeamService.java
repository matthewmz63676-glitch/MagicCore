package com.magicstudios.magiccore.modules.teams;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface TeamService {
    TeamNamePolicy namePolicy();

    CompletionStage<Optional<Team>> teamOf(UUID playerId);

    CompletionStage<TeamMutation> create(UUID ownerId, String name, String operationKey);

    CompletionStage<TeamMutation> rename(UUID actorId, String name, String operationKey);

    CompletionStage<TeamMutation> disband(UUID actorId, String operationKey);

    CompletionStage<TeamMutation> invite(UUID actorId, UUID targetId, String operationKey);

    CompletionStage<TeamMutation> accept(UUID playerId, UUID teamId, String operationKey);

    CompletionStage<TeamMutation> decline(UUID playerId, UUID teamId, String operationKey);

    CompletionStage<TeamMutation> leave(UUID playerId, String operationKey);

    CompletionStage<TeamMutation> kick(UUID actorId, UUID targetId, String operationKey);

    CompletionStage<TeamMutation> setRole(UUID actorId, UUID targetId, TeamRole role, String operationKey);
}
