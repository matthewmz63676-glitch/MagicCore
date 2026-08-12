package com.magicstudios.magiccore.modules.billford;
public record BillfordIngredient(String itemId,int amount){public BillfordIngredient{if(itemId==null||itemId.isBlank()||amount<1)throw new IllegalArgumentException("invalid ingredient");}}
