package com.stockexchange.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Primary key.

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    // One user has exactly one portfolio.
    // @JoinColumn creates a user_id foreign key column in this table.
    // unique = true enforces the one-to-one relationship at DB level.

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvestedValue;
    // Total amount of money currently invested in stocks.
    // = sum of (shares owned × average buy price) for all holdings.
    // Updated every time a trade executes.

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentMarketValue;
    // Current worth of all stock holdings at live prices.
    // = sum of (shares owned × current market price).
    // Changes every time stock prices update.

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalProfitLoss;
    // currentMarketValue - totalInvestedValue.
    // Positive = profit, Negative = loss.
    // Recalculated whenever prices change or trades execute.

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal totalProfitLossPercentage;
    // (totalProfitLoss / totalInvestedValue) * 100.
    // scale = 4 gives 4 decimal places — e.g. 12.3456%.

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
    // When portfolio values were last recalculated.
}