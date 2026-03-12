package com.stockexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class MarketHoursRequest {

    @NotNull(message = "Open time is required")
    private LocalTime openTime;
    // New market open time set by admin.
    // Example: 09:15 — parsed automatically by Jackson from "09:15:00".
    // MarketHoursService saves this to the MarketHours entity.

    @NotNull(message = "Close time is required")
    private LocalTime closeTime;
    // New market close time set by admin.
    // Example: 15:30 — all pending limit orders cancelled at this time.
    // Must be after openTime — validated in MarketHoursService.

    @NotBlank(message = "Timezone is required")
    private String timezone;
    // Timezone for these market hours — "Asia/Kolkata".
    // Ensures market open/close logic works correctly
    // regardless of the server's system timezone.

    private boolean forceClose;
    // If true, admin is manually closing the market immediately
    // regardless of the scheduled close time.
    // Used for emergency market halts.
    // All PENDING orders are cancelled when this is true.
}