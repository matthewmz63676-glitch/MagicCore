package com.magicstudios.magiccore.config.model;

import java.util.List;

public record AuctionFile(int configVersion, String currency, long minimumPriceMinor, long maximumPriceMinor,
                          long listingFeeMinor, long minimumDurationSeconds, long maximumDurationSeconds,
                          List<String> categories) {
    public AuctionFile { categories = List.copyOf(categories); }
}
