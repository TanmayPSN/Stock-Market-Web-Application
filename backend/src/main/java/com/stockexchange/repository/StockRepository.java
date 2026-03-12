package com.stockexchange.repository;

import com.stockexchange.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByTicker(String ticker);
    // Primary lookup for stocks — users place orders using ticker symbols.
    // Used by OrderService to fetch the stock being traded.

    boolean existsByTicker(String ticker);
    // Used by AdminController when adding a new stock
    // to prevent duplicate tickers.

    List<Stock> findByIsActiveTrue();
    // Returns only listed stocks.
    // Used by StockController for the market dashboard —
    // delisted stocks should not appear.

    List<Stock> findByIsActiveFalse();
    // Used by AdminController to view delisted stocks.

    List<Stock> findByCurrentPriceBetween(BigDecimal min, BigDecimal max);
    // Used for filtering stocks by price range on the dashboard.

    @Query("SELECT s FROM Stock s WHERE s.currentPrice <= s.previousClosePrice")
    List<Stock> findDecliningStocks();
    // Returns stocks trading below yesterday's close.
    // Used on dashboard to show red (declining) stocks.

    @Query("SELECT s FROM Stock s WHERE s.currentPrice >= s.previousClosePrice")
    List<Stock> findGainingStocks();
    // Returns stocks trading above yesterday's close.
    // Used on dashboard to show green (gaining) stocks.
}