package com.stockexchange.entity;

import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderStatus;
import com.stockexchange.enums.OrderType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    // Who placed this order.

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    // Which stock this order is for.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;
    // BUY or SELL.
    // Stored as string for readability.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;
    // MARKET or LIMIT.
    // MARKET — execute immediately at current price.
    // LIMIT — wait until price reaches the specified limit price.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    // PENDING   — order placed, not yet executed (limit orders waiting)
    // EXECUTED  — trade happened
    // CANCELLED — user cancelled or market closed with pending orders
    // REJECTED  — failed validation (insufficient balance, market closed, etc.)

    @Column(nullable = false)
    private Integer quantity;
    // Number of shares to buy or sell.

    @Column(precision = 10, scale = 2)
    private BigDecimal limitPrice;
    // Only set for LIMIT orders — the target price.
    // Null for MARKET orders since they execute at whatever the current price is.
    // Order executes when currentPrice <= limitPrice (BUY)
    // or currentPrice >= limitPrice (SELL).

    @Column(precision = 10, scale = 2)
    private BigDecimal executedPrice;
    // The actual price at which the order was filled.
    // For MARKET orders — the price at the moment of execution.
    // For LIMIT orders — the price when condition was met.
    // Null while still PENDING.

    @Column(precision = 15, scale = 2)
    private BigDecimal totalOrderValue;
    // executedPrice × quantity.
    // The actual money that moved. Null while pending.

    @Column(nullable = false)
    private boolean isMarginOrder;
    // Whether this order was placed using margin trading.
    // If true, the MarginAccount is involved in settling this trade.

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;
    // When the user submitted the order.

    private LocalDateTime executedAt;
    // When the order was actually filled. Null until executed.
}