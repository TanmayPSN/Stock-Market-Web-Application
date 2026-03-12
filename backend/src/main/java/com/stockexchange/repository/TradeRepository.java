package com.stockexchange.repository;

import com.stockexchange.entity.Stock;
import com.stockexchange.entity.Trade;
import com.stockexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByBuyerOrderByExecutedAtDesc(User buyer);
    // Returns all trades where this user was the buyer, newest first.
    // Used for trade history on the portfolio page.

    List<Trade> findBySellerOrderByExecutedAtDesc(User seller);
    // Returns all trades where this user was the seller, newest first.

    @Query("SELECT t FROM Trade t WHERE t.buyer = :user OR t.seller = :user " +
            "ORDER BY t.executedAt DESC")
    List<Trade> findAllTradesByUser(@Param("user") User user);
    // Returns complete trade history for a user — both buy and sell sides.
    // Single query instead of two separate calls.

    List<Trade> findByStock(Stock stock);
    // Returns all trades for a specific stock.
    // Used by AdminController for stock-level trade analytics.

    @Query("SELECT t FROM Trade t WHERE t.stock = :stock " +
            "AND t.executedAt >= :since ORDER BY t.executedAt DESC")
    List<Trade> findRecentTradesByStock(@Param("stock") Stock stock,
                                        @Param("since") LocalDateTime since);
    // Returns trades for a stock within a time window.
    // Used to show recent trade activity on the stock detail page.

    @Query("SELECT t FROM Trade t ORDER BY t.executedAt DESC")
    List<Trade> findAllTradesOrderedByDate();
    // Used by AdminController to view all platform trades newest first.
}