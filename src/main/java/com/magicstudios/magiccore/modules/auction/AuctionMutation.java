package com.magicstudios.magiccore.modules.auction;

public record AuctionMutation(boolean applied, String code, AuctionListing listing) { }
