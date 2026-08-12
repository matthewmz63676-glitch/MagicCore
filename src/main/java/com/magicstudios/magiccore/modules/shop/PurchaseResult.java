package com.magicstudios.magiccore.modules.shop;

public record PurchaseResult(boolean applied, String code, long chargedMinor, long balanceAfterMinor) {
}
