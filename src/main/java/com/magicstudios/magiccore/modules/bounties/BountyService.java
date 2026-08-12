package com.magicstudios.magiccore.modules.bounties;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
public interface BountyService{
    enum Sort{VALUE_DESC,NEWEST,CONTRIBUTIONS}
    CompletionStage<BountyMutation> create(UUID creatorId,UUID targetId,long amountMinor,String operationKey);
    CompletionStage<BountyMutation> claim(com.magicstudios.magiccore.modules.combat.VerifiedPlayerKill kill,String operationKey);
    CompletionStage<Optional<Bounty>> active(UUID targetId);
    CompletionStage<List<Bounty>> leaderboard(int limit);
    CompletionStage<List<BountyClaim>> claimHistory(UUID playerId,int limit);
    CompletionStage<BountyPage> search(String query,Sort sort,int page,int pageSize);
}
