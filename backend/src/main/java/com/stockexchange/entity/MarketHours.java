package com.stockexchange.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "market_hours")
public class MarketHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @Column(nullable = false)
    private LocalTime openTime;
    // When market opens — e.g. 09:15:00.
    // Orders cannot be placed before this time.

    @Column(nullable = false)
    private LocalTime closeTime;
    // When market closes — e.g. 15:30:00.
    // All pending limit orders are cancelled at this time.

    @Column(nullable = false)
    private boolean isMarketOpen;
    // Current live status. Admin can manually override this.
    // True = market is open, orders accepted.
    // False = market closed, all orders rejected.

    @Column(nullable = false)
    private String timezone;
    // "Asia/Kolkata" for IST.
    // Stored so the system works correctly even if deployed on servers
    // in different timezones.

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
    // When admin last changed these settings.

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    // Which admin last changed market hours.
    // Audit trail — important for regulated systems.
}
