package com.magicstudios.magiccore.modules.crates;

public record GrantedCrateReward(String rewardId, CrateReward.Type type, String rarity, int sequence,
                                 boolean milestone) { }
