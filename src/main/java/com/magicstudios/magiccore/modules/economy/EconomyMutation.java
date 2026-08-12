package com.magicstudios.magiccore.modules.economy;

public record EconomyMutation(boolean applied, Money resultingBalance, EconomyTransaction transaction) {
}
