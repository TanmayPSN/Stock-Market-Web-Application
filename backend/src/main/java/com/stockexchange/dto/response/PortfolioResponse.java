package com.stockexchange.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PortfolioResponse {

    private String username;
    // Displayed at the top of the portfolio page.

    private BigDecimal availableBalance;
    // Cash the user currently has to place new trades.
    // Decreases on BUY, increases on SELL.

    private BigDecimal totalInvestedValue;
    // Total cash put into stocks at average buy prices.

    private BigDecimal currentMarketValue;
    // Current worth of all holdings at live prices.
    // Changes every time stock prices update.

    private BigDecimal totalProfitLoss;
    // currentMarketValue - totalInvestedValue.
    // Positive = overall profit, Negative = overall loss.

    private BigDecimal totalProfitLossPercentage;
    // (totalProfitLoss / totalInvestedValue) × 100.
    // Shown as "+12.34%" or "-5.67%" on the portfolio page.

    private BigDecimal totalPortfolioValue;
    // availableBalance + currentMarketValue.
    // The user's total net worth in the system.

    private List<HoldingResponse> holdings;
    // List of all stocks currently owned.
    // Each item is a HoldingResponse (nested DTO below).

    private boolean marginEnabled;
    // Whether user has margin trading active.
    // If true, frontend shows the margin section on portfolio page.

    private BigDecimal marginUsed;
    // How much borrowed margin is currently in use.
    // Only shown if marginEnabled = true.

    private BigDecimal marginAvailable;
    // How much more margin the user can still borrow.

    private boolean marginCallTriggered;
    // If true, frontend shows a red margin call warning banner.

    private LocalDateTime lastUpdatedAt;
    // When portfolio was last recalculated.

    // ── Nested DTO ───────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class HoldingResponse {
        // Nested inside PortfolioResponse to avoid a separate file
        // since it is only ever used as part of a portfolio.

        private String ticker;
        // Stock symbol — "TCS".

        private String companyName;
        // "Tata Consultancy Services".

        private Integer quantityOwned;
        // How many shares the user holds.

        private BigDecimal averageBuyPrice;
        // Average price paid per share across all buys.

        private BigDecimal currentPrice;
        // Live price right now — used to show unrealized P&L.

        private BigDecimal currentValue;
        // quantityOwned × currentPrice.
        // What these shares are worth right now.

        private BigDecimal investedAmount;
        // quantityOwned × averageBuyPrice.
        // What the user paid for these shares.

        private BigDecimal profitLoss;
        // currentValue - investedAmount.

        private BigDecimal profitLossPercent;
        // (profitLoss / investedAmount) × 100.
        // Shown as green or red on each holding row.

        private String priceDirection;
        // "UP" or "DOWN" compared to previous close.
    }
}