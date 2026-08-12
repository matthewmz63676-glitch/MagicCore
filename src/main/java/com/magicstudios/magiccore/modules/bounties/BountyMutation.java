package com.magicstudios.magiccore.modules.bounties;
public record BountyMutation(boolean applied,String code,Bounty bounty,BountyContribution contribution,BountyClaim claim){}
