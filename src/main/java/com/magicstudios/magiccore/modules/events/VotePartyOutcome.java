package com.magicstudios.magiccore.modules.events;

import java.util.Optional;

public record VotePartyOutcome(VerifiedVote vote,VotePartyState state,Optional<PinataParty>triggeredParty){public VotePartyOutcome{triggeredParty=triggeredParty==null?Optional.empty():triggeredParty;}}
