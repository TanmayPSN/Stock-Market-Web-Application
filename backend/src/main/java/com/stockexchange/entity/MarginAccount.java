package com.stockexchange.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "margin_accounts")
public class MarginAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    // One user has one margin account.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginLimit;
    // Maximum additional buying power granted beyond actual balance.
    // Example: balance = 10,000 and marginLimit = 40,000
    // means total trading power = 50,000 (5x margin).

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginUsed;
    // How much margin the user has currently borrowed and is using.
    // marginUsed increases with every margin trade.
    // Must never exceed marginLimit.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginAvailable;
    // marginLimit - marginUsed.
    // How much more margin the user can still use.
    // Recalculated after every margin trade.

    @Column(nullable = false)
    private BigDecimal marginMultiplier;
    // The leverage factor — 5 means 5x margin.
    // Admin sets this per user or globally.
    // marginLimit = user.balance × marginMultiplier.

    @Column(nullable = false)
    private boolean marginCallTriggered;
    // True when losses exceed a threshold (e.g. 80% of margin used).
    // When true, user gets notified and must add funds or close positions.
    // System may auto-close positions to recover margin.

    @Column(precision = 10, scale = 2)
    private BigDecimal marginCallThreshold;
    // The loss level that triggers a margin call.
    // Example: if marginUsed = 40,000 and threshold = 80%,
    // margin call triggers when losses hit 32,000.

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
    // When margin values were last recalculated.
}