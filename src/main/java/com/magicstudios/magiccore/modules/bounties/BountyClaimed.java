package com.magicstudios.magiccore.modules.bounties;
import com.magicstudios.magiccore.api.DomainEvent;
import java.time.Instant;
import java.util.UUID;
public record BountyClaimed(UUID targetId,UUID killerId,long payoutMinor,String currency,String operationKey,
                            Instant occurredAt)implements DomainEvent{}
