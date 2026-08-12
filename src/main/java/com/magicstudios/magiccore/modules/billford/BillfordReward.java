package com.magicstudios.magiccore.modules.billford;
public record BillfordReward(String id,Type type,int weight,String currency,long amountMinor,String material,int amount,String itemDataBase64){public enum Type{CURRENCY,ITEM}}
