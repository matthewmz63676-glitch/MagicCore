package com.magicstudios.magiccore.modules.shop;

public record SellResult(boolean applied, String code, long creditedMinor, long balanceAfterMinor) { }
