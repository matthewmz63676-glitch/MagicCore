package com.magicstudios.magiccore.modules.worth;

import java.util.List;
import java.util.Optional;

public interface ItemValuationService {
    String currency();
    ItemValuation value(ValuationInput input);
    Optional<ItemValuation> unitValue(String itemId);
    List<String> categories();
    String render(ItemValuation valuation);
}
