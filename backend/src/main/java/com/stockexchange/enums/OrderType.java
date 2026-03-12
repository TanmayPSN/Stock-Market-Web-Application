package com.stockexchange.enums;

public enum OrderType {
    MARKET,
    // Execute immediately at whatever the current market price is.
    // No price condition — just buy/sell right now.
    // Example: BUY 10 TCS at whatever TCS is trading at right now.
    // These orders never stay PENDING — they execute or get rejected instantly.

    LIMIT
    // Execute only when the stock reaches a specific target price.
    // Example: BUY TCS only when price drops to 3480.
    // Order stays PENDING until price condition is met.
    // If market closes before condition is met, order is CANCELLED.
}