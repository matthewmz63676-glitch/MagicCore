package com.magicstudios.magiccore.modules.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface VotePartyService {
    CompletionStage<VotePartyOutcome> recordVerifiedVote(String providerEventId,UUID playerId,String service,Instant occurredAt,boolean playerOnline);
    CompletionStage<VotePartyState> state();
    CompletionStage<Optional<PinataParty>> activeParty();
    CompletionStage<List<PinataParty>> pendingParties(int limit);
    CompletionStage<PinataParty> activate(UUID partyId,String operationKey);
    CompletionStage<PinataParty> hit(UUID partyId,UUID playerId,String operationKey);
    CompletionStage<PinataParty> cancel(UUID partyId,String operationKey);
}
