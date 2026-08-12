package com.magicstudios.magiccore.admin;
import com.magicstudios.magiccore.audit.AuditEvent;
import com.magicstudios.magiccore.audit.AuditService;
import com.magicstudios.magiccore.modules.auction.AuctionService;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceAnalyticsService;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceSnapshot;
import com.magicstudios.magiccore.modules.orders.OrderService;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
public final class DefaultMarketplaceAdminService implements MarketplaceAdminService{
 private final CapabilityGate gate;private final MarketplaceAnalyticsService analytics;private final AuctionService auctions;private final OrderService orders;private final AuditService audit;private final Clock clock;
 public DefaultMarketplaceAdminService(CapabilityGate gate,MarketplaceAnalyticsService analytics,AuctionService auctions,OrderService orders,AuditService audit,Clock clock){this.gate=gate;this.analytics=analytics;this.auctions=auctions;this.orders=orders;this.audit=audit;this.clock=clock;}
 @Override public CompletionStage<MarketplaceSnapshot> snapshot(AdminActor actor){return gate.has(actor,"VIEW_DIAGNOSTICS").thenCompose(allowed->{if(!allowed)throw new SecurityException("VIEW_DIAGNOSTICS required");return analytics.snapshot();});}
 @Override public CompletionStage<ExpiryRun> expireDue(AdminActor actor,String operationKey,int limit){return gate.has(actor,"MANAGE_SHOPS").thenCompose(allowed->{if(!allowed)throw new SecurityException("MANAGE_SHOPS required");
  return auctions.expire(operationKey+":auctions",limit).thenCombine(orders.expire(operationKey+":orders",limit),(a,o)->new ExpiryRun(a,o));}).thenCompose(run->audit.record(new AuditEvent(UUID.randomUUID(),operationKey,"MARKETPLACE_EXPIRE",actor.displayName(),"marketplace",Map.of(),Map.of("auctions",Integer.toString(run.auctions()),"orders",Integer.toString(run.orders())),"admin-service",clock.instant())).thenApply(ignored->run));}
}
