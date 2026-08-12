package com.magicstudios.magiccore.integrations.discord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface CustomDiscordBridge extends DiscordIntegrationService {
 CompletionStage<DiscordLinkCodeIssue>issueLinkCode(UUID playerId,String operationKey);
 CompletionStage<DiscordLink>redeemLinkCode(String code,String discordId,String operationKey);
 CompletionStage<Boolean>unlink(UUID playerId,String operationKey);
 BridgeEnvelope sign(String type,String payload,String nonce);
 CompletionStage<Boolean>accept(BridgeEnvelope envelope,String source);
 CompletionStage<List<BridgeOutboxMessage>>pendingOutbox(int limit);
 CompletionStage<Boolean>acknowledge(UUID messageId,String operationKey);
 CompletionStage<BridgeOutboxMessage>markFailed(UUID messageId,String error,String operationKey);
 CompletionStage<DiscordBridgeHealth>bridgeHealth();
}
