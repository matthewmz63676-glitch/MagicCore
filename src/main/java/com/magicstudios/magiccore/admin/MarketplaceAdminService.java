package com.magicstudios.magiccore.admin;
import com.magicstudios.magiccore.modules.marketplace.MarketplaceSnapshot;
import java.util.concurrent.CompletionStage;
public interface MarketplaceAdminService{
 CompletionStage<MarketplaceSnapshot> snapshot(AdminActor actor);
 CompletionStage<ExpiryRun> expireDue(AdminActor actor,String operationKey,int limit);
 record ExpiryRun(int auctions,int orders){}
}
