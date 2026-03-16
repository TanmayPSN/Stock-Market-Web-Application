package com.stockexchange.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class StockResponse {

    private Long id;
    // Stock's database ID — used by frontend when placing orders
    // to reference a specific stock.

    private String ticker;
    // "TCS", "INFY", "RELIANCE" — displayed on stock cards.

    private String companyName;
    // "Tata Consultancy Services" — displayed below the ticker.

    private BigDecimal currentPrice;
    // Live price — updated every 5 seconds by StockPriceSimulator.
    // Frontend displays this as the main price on each stock card.

    private BigDecimal openPrice;
    // Price at market open — used to calculate intraday change.

    private BigDecimal highPrice;
    // Day's highest price — shown in stock detail view.

    private BigDecimal lowPrice;
    // Day's lowest price — shown in stock detail view.

    private BigDecimal previousClosePrice;
    // Yesterday's close — used to calculate overnight change %.

    private BigDecimal priceChangeAmount;
    // currentPrice - previousClosePrice.
    // Positive = gained, Negative = lost.
    // Calculated in StockService before building this response.

    private BigDecimal priceChangePercent;
    // (priceChangeAmount / previousClosePrice) × 100.
    // Frontend shows this as "+1.23%" or "-0.87%".

    private String priceDirection;
    // "UP" or "DOWN" — frontend uses this to show green ↑ or red ↓ arrow.

    private Long totalSharesAvailable;
    // Remaining shares that can be bought.
    // Shown on order form so user knows availability.

    private boolean active;
    // If false, stock is delisted — order form disables BUY/SELL buttons.

    private LocalDateTime lastUpdatedAt;
    // When price was last changed by the simulator.
    // Frontend shows "Updated X seconds ago".
}