package com.stockexchange.enums;

public enum OrderStatus {
    PENDING,
    // Order has been placed but not yet executed.
    // Only LIMIT orders can be in this state —
    // they wait until the stock price hits the limit price.
    // MARKET orders skip this state entirely.

    EXECUTED,
    // Trade has been completed successfully.
    // Balance and portfolio holdings have been updated.
    // A Trade record has been created for this order.

    CANCELLED,
    // Order was cancelled before execution.
    // Two scenarios:
    // 1. User manually cancelled a pending limit order.
    // 2. Market closed and the limit order never got filled —
    //    system auto-cancels all PENDING orders at market close.

    REJECTED,
    // Order failed validation and was never processed.
    // Reasons include:
    // - Market is closed when order was placed
    // - Insufficient balance for a BUY order
    // - Insufficient shares for a SELL order
    // - Margin limit exceeded
    // - Stock is delisted (isActive = false)
    // - Invalid quantity (zero or negative)
}