package com.magicstudios.magiccore.modules.store;

public record PurchaseResult(boolean accepted, boolean replay, String code, PurchaseRecord purchase) { }
