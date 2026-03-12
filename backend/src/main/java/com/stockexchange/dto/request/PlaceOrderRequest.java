package com.stockexchange.dto.request;

import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotBlank(message = "Ticker is required")
    private String ticker;
    // Stock symbol the user wants to trade — "TCS", "INFY", "RELIANCE".
    // OrderService uses this to look up the stock via StockRepository.
    // Validated to exist in the database before order is processed.

    @NotNull(message = "Order side is required")
    private OrderSide side;
    // BUY or SELL.
    // OrderService uses this to determine whether to
    // deduct balance (BUY) or return balance (SELL).

    @NotNull(message = "Order type is required")
    private OrderType type;
    // MARKET or LIMIT.
    // MARKET — execute immediately at current price, limitPrice is ignored.
    // LIMIT  — limitPrice below is required and must be set.

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    // Number of shares to buy or sell.
    // @Min(1) prevents zero or negative quantity orders.

    private BigDecimal limitPrice;
    // Only required when type = LIMIT.
    // For BUY  limit — order executes when price drops to this value.
    // For SELL limit — order executes when price rises to this value.
    // Null for MARKET orders — OrderService ignores it.
    // OrderService validates this is not null when type = LIMIT.

    private boolean useMargin;
    // Whether the user wants to use margin for this trade.
    // If true, MarginService is involved in balance and limit checks.
    // If false, only the user's actual balance is used.
}