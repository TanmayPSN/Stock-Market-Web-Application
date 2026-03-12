package com.stockexchange.enums;

public enum MarketStatus {
    OPEN,
    // Market is accepting orders.
    // Stock price simulator is actively running.
    // Time is between openTime and closeTime in MarketHours.

    CLOSED,
    // Market is not accepting orders.
    // All incoming orders will be REJECTED.
    // Stock price simulator is paused.
    // All PENDING limit orders are auto-cancelled at this transition.

    PRE_MARKET,
    // Before official open time (before 9:15 AM).
    // Orders are rejected but users can view prices.
    // Useful to show countdown to market open on dashboard.

    POST_MARKET
    // After official close time (after 3:30 PM).
    // Same as PRE_MARKET — no orders accepted.
    // Shows final closing prices of the day.
}