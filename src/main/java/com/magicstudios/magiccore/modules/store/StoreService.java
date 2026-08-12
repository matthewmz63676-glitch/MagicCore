package com.magicstudios.magiccore.modules.store;

import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface StoreService {
    String url();
    Map<String, ProductDefinition> products();
    CompletionStage<PurchaseResult> accept(PurchaseRequest request, String signature);
    CompletionStage<DonationGoalState> donationGoal();
}
