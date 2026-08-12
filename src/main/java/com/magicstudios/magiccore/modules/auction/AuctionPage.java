package com.magicstudios.magiccore.modules.auction;

import java.util.List;

public record AuctionPage(List<AuctionListing> listings, int page, int pageSize, long total) {
    public AuctionPage { listings = List.copyOf(listings); }
}
