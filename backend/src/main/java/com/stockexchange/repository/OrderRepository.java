package com.stockexchange.repository;

import com.stockexchange.entity.Order;
import com.stockexchange.entity.Stock;
import com.stockexchange.entity.User;
import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderStatus;
import com.stockexchange.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);
    // Returns all orders placed by a user — used for order history page.

    List<Order> findByUserAndStatus(User user, OrderStatus status);
    // Used to show a user's pending limit orders specifically
    // so they can cancel them if needed.

    List<Order> findByStatus(OrderStatus status);
    // Used by OrderService to fetch all PENDING limit orders
    // across all users — checked on every price update to see
    // if any limit conditions have been met.

    List<Order> findByStockAndStatusAndSide(Stock stock, OrderStatus status, OrderSide side);
    // Used by TradeService to find matching orders —
    // e.g. all PENDING BUY orders for TCS to match against a SELL.

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PARTIAL') " +
            "AND o.type = 'LIMIT' " +
            "AND o.side = 'BUY' " +
            "AND o.stock.id = :stockId " +
            "AND o.limitPrice >= :currentPrice")
    List<Order> findEligibleBuyLimitOrders(@Param("stockId") Long stockId,
                                           @Param("currentPrice") BigDecimal currentPrice);
    // Finds BUY limit orders that can now execute because
    // the stock price has dropped to or below their limit price.
    // Called by StockPriceSimulator on every price update.

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PARTIAL') " +
            "AND o.type = 'LIMIT' " +
            "AND o.side = 'SELL' " +
            "AND o.stock.id = :stockId " +
            "AND o.limitPrice <= :currentPrice")
    List<Order> findEligibleSellLimitOrders(@Param("stockId") Long stockId,
                                            @Param("currentPrice") BigDecimal currentPrice);
    // Finds SELL limit orders that can now execute because
    // the stock price has risen to or above their limit price.

    List<Order> findByUserOrderByPlacedAtDesc(User user);
    // Returns user's orders sorted newest first.
    // Used for the order history display on the frontend.

    // Find the best SELL order to match against a BUY
    @Query("SELECT o FROM Order o WHERE o.stock.id = :stockId " +
            "AND o.side = 'SELL' " +
            "AND o.status IN ('PENDING', 'PARTIAL') " +
            "AND o.user.id != :userId " +
            "AND (o.type = 'MARKET' OR o.limitPrice <= :buyPrice) " +
            "AND (o.quantity - o.filledQuantity) > 0 " +
            "ORDER BY o.limitPrice ASC, o.placedAt ASC")
    Optional<Order> findBestSellMatch(@Param("stockId") Long stockId,
                                      @Param("buyPrice") BigDecimal buyPrice,
                                      @Param("userId") Long userId);

    // Find the best BUY order to match against a SELL
    @Query("SELECT o FROM Order o WHERE o.stock.id = :stockId " +
            "AND o.side = 'BUY' " +
            "AND o.status IN ('PENDING', 'PARTIAL') " +
            "AND o.user.id != :userId " +
            "AND (o.type = 'MARKET' OR o.limitPrice >= :sellPrice) " +
            "AND (o.quantity - o.filledQuantity) > 0 " +
            "ORDER BY o.limitPrice DESC, o.placedAt ASC")
    Optional<Order> findBestBuyMatch(@Param("stockId") Long stockId,
                                     @Param("sellPrice") BigDecimal sellPrice,
                                     @Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'PARTIAL')")
    List<Order> findAllPendingOrders();
    // Used at market close to cancel all remaining pending and partial limit orders.
}