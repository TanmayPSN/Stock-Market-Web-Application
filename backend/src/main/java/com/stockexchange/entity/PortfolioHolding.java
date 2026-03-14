package com.stockexchange.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_holdings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "stock_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    // Which portfolio this holding belongs to.
    // ManyToOne — one portfolio can have many holdings (TCS, INFY, HDFC...).

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
    // Which stock is being held.
    // Combined with portfolio_id, the uniqueConstraint above ensures
    // one user cannot have two rows for the same stock.

    @Column(nullable = false)
    private Integer quantityOwned;
    // How many shares the user currently holds.
    // Increases on BUY, decreases on SELL.
    // If it reaches 0, the row can be deleted or kept at 0.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal averageBuyPrice;
    // Weighted average price at which shares were bought.
    // Example: Buy 5 @ 3500, then buy 5 more @ 3600
    // Average = (5×3500 + 5×3600) / 10 = 3550
    // Used to calculate profit/loss on this holding.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalInvestedAmount;
    // quantityOwned × averageBuyPrice.
    // Stored separately so profit/loss calculation is fast —
    // no need to look up trade history every time.

    @Column(nullable = false)
    private boolean marginPosition;
    // True if this holding was bought using margin.
    // Used at sell time to decide where proceeds go —
    // back to balance or back to margin account.

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
    // When this holding was last changed (buy or sell executed).
}
