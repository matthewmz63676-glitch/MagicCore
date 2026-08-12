package com.magicstudios.magiccore.config.model;
public record BountiesFile(int configVersion,String currency,long minimumAmountMinor,long maximumAmountMinor,
                           int taxBasisPoints,int maximumContributionsPerTarget,Restrictions restrictions){
 public BountiesFile{if(restrictions==null)restrictions=new Restrictions(false,0,0);}
 public record Restrictions(boolean enabled,long minimumTargetPlaytimeSeconds,long minimumTargetKills){}
}
