package com.stockexchange.repository;

import com.stockexchange.entity.Portfolio;
import com.stockexchange.entity.PortfolioHolding;
import com.stockexchange.entity.Stock;
import com.stockexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {

    List<PortfolioHolding> findByPortfolio(Portfolio portfolio);
    // Returns all stock holdings for a portfolio.
    // Used by PortfolioService to display all stocks a user owns.

    Optional<PortfolioHolding> findByPortfolioAndStock(Portfolio portfolio, Stock stock);
    // Finds a specific stock holding within a portfolio.
    // Used by OrderService when executing a trade —
    // to update quantity and average price of the affected stock.

    Optional<PortfolioHolding> findByPortfolioIdAndStockId(Long portfolioId, Long stockId);
    // Same as above but using IDs directly —
    // avoids loading full Portfolio and Stock objects unnecessarily.

    List<PortfolioHolding> findByPortfolioIdAndQuantityOwnedGreaterThan(
            Long portfolioId, Integer quantity);
    // Returns holdings where user actually owns shares (quantity > 0).
    // Used to display active holdings only — filters out zero quantity rows.

    @Query("SELECT ph FROM PortfolioHolding ph WHERE ph.portfolio.id = :portfolioId " +
            "ORDER BY ph.totalInvestedAmount DESC")
    List<PortfolioHolding> findTopHoldingsByPortfolioId(@Param("portfolioId") Long portfolioId);
    // Returns holdings sorted by invested amount, highest first.
    // Used on portfolio page to show biggest positions at the top.

    @Query("SELECT h FROM PortfolioHolding h WHERE h.portfolio.user = :user AND h.marginPosition = true")
    List<PortfolioHolding> findByPortfolioUserAndMarginPositionTrue(@Param("user") User user);

    boolean existsByPortfolioAndStock(Portfolio portfolio, Stock stock);
    // Used before creating a new holding —
    // if it exists already, update it instead of inserting a new row.
}