package com.stockexchange.enums;

public enum OrderSide {
    BUY,
    // User wants to purchase shares.
    // System checks: user balance >= quantity × price
    // On execution: deduct balance, increase portfolio holding

    SELL
    // User wants to sell shares they own.
    // System checks: user owns enough shares to sell
    // On execution: increase balance, decrease portfolio holding
}