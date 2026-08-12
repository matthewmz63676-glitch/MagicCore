package com.magicstudios.magiccore.modules.orders;

public record OrderMutation(boolean applied, String code, BuyOrder order, OrderFulfillment fulfillment) { }
