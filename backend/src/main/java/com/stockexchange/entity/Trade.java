package com.stockexchange.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @ManyToOne
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;
    // The order that was on the BUY side of this trade.
    // Links back to the full order details.

    @ManyToOne
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;
    // The order that was on the SELL side of this trade.

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    // Which stock was traded. Denormalized here for fast trade history queries
    // without joining through orders every time.

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    // The user who bought. Stored directly for fast "my trade history" queries.

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    // The user who sold.

    @Column(nullable = false)
    private Integer quantity;
    // Number of shares that changed hands in this trade.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal executedPrice;
    // Price per share at which the trade settled.

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTradeValue;
    // executedPrice × quantity. Total money transferred from buyer to seller.

    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;
    // Exact timestamp when the trade completed.
    // Used in trade history timeline and analytics.
}
