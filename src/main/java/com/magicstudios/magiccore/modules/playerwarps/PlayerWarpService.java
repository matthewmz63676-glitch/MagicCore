package com.magicstudios.magiccore.modules.playerwarps;

import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.time.Duration;

public interface PlayerWarpService {
    CompletionStage<List<PlayerWarp>> activeWarps();
    CompletionStage<List<PlayerWarp>> ownedBy(UUID ownerId);
    CompletionStage<Optional<PlayerWarp>> findActive(String warpId);
    CompletionStage<PlayerWarpMutation> create(UUID ownerId, String name, String category,
                                               WorldPosition position, String operationKey);
    CompletionStage<PlayerWarpMutation> delete(UUID ownerId, String warpId, String operationKey);
    CompletionStage<Boolean> recordVisit(String warpId, UUID visitorId, String operationKey);
    CompletionStage<PlayerWarp> prepareVisit(String warpId,UUID visitorId,String operationKey);
    CompletionStage<List<PlayerWarpView>> search(PlayerWarpQuery query);
    CompletionStage<Boolean> favorite(UUID playerId,String warpId,boolean favorite,String operationKey);
    CompletionStage<PlayerWarpMutation> moderate(UUID actorId,String warpId,PlayerWarp.Status status,String reason,String operationKey);
    CompletionStage<PlayerWarpMutation> transfer(UUID ownerId,String warpId,UUID newOwnerId,String operationKey);
    CompletionStage<WarpSponsorship> sponsor(UUID playerId,String warpId,Duration duration,String operationKey);
    CompletionStage<WarpSponsorship> cancelSponsorship(UUID playerId,UUID sponsorshipId,String operationKey);
    CompletionStage<Integer> expireSponsorships();
}
