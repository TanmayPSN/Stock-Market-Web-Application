package com.stockexchange.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @Column(nullable = false, unique = true)
    private String ticker;
    // The short symbol — "TCS", "INFY", "RELIANCE".
    // unique = true because no two stocks can share a ticker.
    // This is what users type when placing orders.

    @Column(nullable = false)
    private String companyName;
    // Full name — "Tata Consultancy Services".
    // Displayed on the dashboard next to the ticker.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;
    // The live price right now. Updated by StockPriceSimulator every few seconds.
    // precision = 10, scale = 2 means up to 99999999.99 — enough for any stock.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openPrice;
    // The price at market open (9:15 AM).
    // Used to calculate % change during the day: ((current - open) / open) * 100.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal highPrice;
    // Highest price reached today. Updated whenever currentPrice exceeds it.
    // Shown on dashboard as the day's high.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lowPrice;
    // Lowest price reached today. Updated whenever currentPrice goes below it.
    // Shown on dashboard as the day's low.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal previousClosePrice;
    // Yesterday's closing price.
    // Used to calculate overnight change when market opens.

    @Column(nullable = false)
    private Long totalSharesAvailable;
    // Total shares available in the exchange for this stock.
    // Decreases when users buy, increases when users sell.

    @Column(name = "is_active", nullable = false)
    private boolean active;
    // Admin can delist a stock. If false, no orders can be placed on it.

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
    // Timestamp of the last price update by the simulator.
    // Frontend uses this to show "last updated X seconds ago".
}