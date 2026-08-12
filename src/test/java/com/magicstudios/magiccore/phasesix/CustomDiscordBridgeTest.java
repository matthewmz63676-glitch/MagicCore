package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.integrations.discord.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomDiscordBridgeTest {
    @Test void oneTimeCodesEnforcePlayerAndDiscordUniquenessAndRevocation(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"discord-link"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));var bridge=bridge(store,clock,10,3);UUID first=UUID.randomUUID(),second=UUID.randomUUID();DiscordLinkCodeIssue issue=bridge.issueLinkCode(first,"issue-1").toCompletableFuture().join();DiscordLink link=bridge.redeemLinkCode(issue.code(),"123456789012345678","redeem-1").toCompletableFuture().join();assertThat(link.playerId()).isEqualTo(first);assertThat(bridge.linkedDiscordId(first).toCompletableFuture().join()).contains("123456789012345678");assertThatThrownBy(()->bridge.redeemLinkCode(issue.code(),"999999999999999999","replay-code").toCompletableFuture().join()).hasRootCauseMessage("LINK_CODE_EXPIRED_OR_USED");DiscordLinkCodeIssue secondCode=bridge.issueLinkCode(second,"issue-2").toCompletableFuture().join();assertThatThrownBy(()->bridge.redeemLinkCode(secondCode.code(),"123456789012345678","duplicate-discord").toCompletableFuture().join()).hasRootCauseMessage("DISCORD_ACCOUNT_ALREADY_LINKED");assertThat(bridge.unlink(first,"unlink").toCompletableFuture().join()).isTrue();assertThat(bridge.linkedDiscordId(first).toCompletableFuture().join()).isEmpty();assertThat(bridge.redeemLinkCode(secondCode.code(),"123456789012345678","after-unlink").toCompletableFuture().join().playerId()).isEqualTo(second);}finally{store.close();}}

    @Test void signaturesReplayRateLimitsAndBoundedOutboxRetryFailClosed(){var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"discord-sign"));try{MutableClock clock=new MutableClock(Instant.parse("2026-08-11T12:00:00Z"));var bridge=bridge(store,clock,2,2);BridgeEnvelope signed=bridge.sign("STATS_REQUEST","{}","nonce_12345678");assertThat(bridge.accept(signed,"bot-a").toCompletableFuture().join()).isTrue();assertThatThrownBy(()->bridge.accept(signed,"bot-a").toCompletableFuture().join()).hasRootCauseMessage("BRIDGE_REPLAY");BridgeEnvelope invalid=new BridgeEnvelope(signed.id(),"another_nonce_123","STATS_REQUEST","{}",signed.timestamp(),"00");assertThatThrownBy(()->bridge.accept(invalid,"bot-b").toCompletableFuture().join()).hasRootCauseMessage("BRIDGE_SIGNATURE_INVALID");bridge.notify("hello").toCompletableFuture().join();BridgeOutboxMessage message=bridge.pendingOutbox(10).toCompletableFuture().join().getFirst();assertThat(bridge.markFailed(message.id(),"network", "fail-1").toCompletableFuture().join().status()).isEqualTo(BridgeOutboxMessage.Status.PENDING);clock.advance(Duration.ofSeconds(5));assertThat(bridge.markFailed(message.id(),"network", "fail-2").toCompletableFuture().join().status()).isEqualTo(BridgeOutboxMessage.Status.DEAD);assertThat(bridge.bridgeHealth().toCompletableFuture().join().dead()).isEqualTo(1);}finally{store.close();}}

    private static PersistentCustomDiscordBridge bridge(InMemoryTransactionalDataStore store,Clock clock,int rate,int retries){return new PersistentCustomDiscordBridge(store,clock,"0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8),Duration.ofMinutes(10),Duration.ofSeconds(60),rate,retries,Duration.ofSeconds(5));}
    private static final class MutableClock extends Clock{private Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration value){now=now.plus(value);}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId zone){return this;}@Override public Instant instant(){return now;}}
}
