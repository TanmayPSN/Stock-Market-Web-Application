package com.stockexchange.service;

import com.stockexchange.dto.request.PlaceOrderRequest;
import com.stockexchange.dto.response.OrderResponse;
import com.stockexchange.entity.*;
import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderStatus;
import com.stockexchange.enums.OrderType;
import com.stockexchange.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository            orderRepository;
    private final UserRepository             userRepository;
    private final StockRepository            stockRepository;
    private final PortfolioRepository        portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final MarketHoursService         marketHoursService;
    private final MarginService              marginService;
    private final TradeService               tradeService;

    // ── Place Order ──────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User  user  = getCurrentUser();
        Stock stock = stockRepository
                .findByTicker(request.getTicker().toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Stock not found: " + request.getTicker()));

        // Step 1 — Market open check
        if (!marketHoursService.isMarketOpen()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Market is closed");
        }

        // Step 2 — Stock active check
        if (!stock.isActive()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Stock is delisted");
        }

        // Step 3 — Side-specific validation
        if (request.getSide() == OrderSide.BUY) {
            return processBuyOrder(user, stock, request);
        } else {
            return processSellOrder(user, stock, request);
        }
    }

    // ── Buy Order ────────────────────────────────────────────────────────────

    private OrderResponse processBuyOrder(User user, Stock stock,
                                          PlaceOrderRequest request) {
        BigDecimal price = request.getType() == OrderType.MARKET
                ? stock.getCurrentPrice()
                : request.getLimitPrice();

        BigDecimal totalCost = price.multiply(
                BigDecimal.valueOf(request.getQuantity()));

        // Validate limit price is set for limit orders
        if (request.getType() == OrderType.LIMIT
                && request.getLimitPrice() == null) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Limit price required for limit orders");
        }

        // Check available shares
        if (stock.getTotalSharesAvailable() < request.getQuantity()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Insufficient shares available");
        }

        // Balance check
        if (request.isUseMargin()) {
            if (!marginService.hasSufficientMargin(user, totalCost)) {
                return rejectOrder(user, stock, request,
                        "Order Rejected: Insufficient margin available");
            }
        } else {
            if (user.getBalance().compareTo(totalCost) < 0) {
                return rejectOrder(user, stock, request,
                        "Order Rejected: Insufficient balance. Required: "
                                + totalCost + ", Available: "
                                + user.getBalance());
            }
        }

        // Create and save the order
        Order order = buildOrder(user, stock, request);
        Order savedOrder = orderRepository.save(order);

        // Market order — execute immediately
        if (request.getType() == OrderType.MARKET) {
            tradeService.executeTrade(savedOrder,
                    stock.getCurrentPrice());
        }
        // Limit order — stays PENDING until price condition is met

        log.info("Order placed: {} {} {} x{} by {}",
                request.getSide(), request.getType(),
                stock.getTicker(), request.getQuantity(),
                user.getUsername());

        return toOrderResponse(savedOrder);
    }

    // ── Sell Order ───────────────────────────────────────────────────────────
    private OrderResponse processSellOrder(User user, Stock stock,
                                           PlaceOrderRequest request) {
        // Validate limit price for limit sell orders
        if (request.getType() == OrderType.LIMIT
                && request.getLimitPrice() == null) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Limit price required for limit orders");
        }

        // Check user owns enough shares to sell
        PortfolioHolding holding = holdingRepository
                .findByPortfolioIdAndStockId(
                        getUserPortfolioId(user), stock.getId())
                .orElse(null);

        if (holding == null || getOwnedQuantity(user, stock)
                < request.getQuantity()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Insufficient shares to sell");
        }

        Order order = buildOrder(user, stock, request);
        Order savedOrder = orderRepository.save(order);

        if (request.getType() == OrderType.MARKET) {
            tradeService.executeTrade(savedOrder,
                    stock.getCurrentPrice());
        }

        return toOrderResponse(savedOrder);
    }

    // ── Cancel Order ─────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User  user  = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "Cannot cancel another user's order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Only PENDING orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toOrderResponse(orderRepository.save(order));
    }

    // ── Limit Order Execution Check ──────────────────────────────────────────

    @Transactional
    public void checkAndExecuteLimitOrders(Long stockId,
                                           BigDecimal currentPrice) {
        // Called by StockPriceSimulator on every price tick.
        // Checks if any pending limit orders can now execute.

        // BUY limit orders — execute when price drops to limit price
        orderRepository
                .findEligibleBuyLimitOrders(stockId, currentPrice)
                .forEach(order -> tradeService.executeTrade(
                        order, currentPrice));

        // SELL limit orders — execute when price rises to limit price
        orderRepository
                .findEligibleSellLimitOrders(stockId, currentPrice)
                .forEach(order -> tradeService.executeTrade(
                        order, currentPrice));
    }

    // ── Market Close — Cancel All Pending ────────────────────────────────────

    @Transactional
    public void cancelAllPendingOrdersAtMarketClose() {
        // Called by MarketHoursService when market closes.
        // All limit orders that never got filled are cancelled.
        List<Order> pendingOrders = orderRepository.findAllPendingOrders();
        pendingOrders.forEach(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });
        log.info("Cancelled {} pending orders at market close",
                pendingOrders.size());
    }

    // ── User Order History ───────────────────────────────────────────────────

    public List<OrderResponse> getMyOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserOrderByPlacedAtDesc(user)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    public List<OrderResponse> getMyPendingOrders() {
        User user = getCurrentUser();
        return orderRepository
                .findByUserAndStatus(user, OrderStatus.PENDING)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Order buildOrder(User user, Stock stock,
                             PlaceOrderRequest request) {
        Order order = new Order();
        order.setUser(user);
        order.setStock(stock);
        order.setSide(request.getSide());
        order.setType(request.getType());
        order.setQuantity(request.getQuantity());
        order.setLimitPrice(request.getLimitPrice());
        order.setMarginOrder(request.isUseMargin());
        order.setStatus(request.getType() == OrderType.MARKET
                ? OrderStatus.PENDING
                : OrderStatus.PENDING);
        // Both start as PENDING — TradeService immediately
        // executes MARKET orders after this.
        order.setPlacedAt(LocalDateTime.now());
        return order;
    }

    private OrderResponse rejectOrder(User user, Stock stock,
                                      PlaceOrderRequest request,
                                      String reason) {
        Order order = buildOrder(user, stock, request);
        order.setStatus(OrderStatus.REJECTED);
        Order saved = orderRepository.save(order);
        log.warn("Order rejected for user {}: {}", user.getUsername(), reason);
        OrderResponse response = toOrderResponse(saved);
        response.setRejectionReason(reason);
        return response;
    }

    private int getOwnedQuantity(User user, Stock stock) {
        return holdingRepository
                .findByPortfolioIdAndStockId(
                        getUserPortfolioId(user), stock.getId())
                .map(PortfolioHolding::getQuantityOwned)
                .orElse(0);
    }

    private Long getUserPortfolioId(User user) {
        return portfolioRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Portfolio not found for user: "
                                + user.getUsername()))
                .getId();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));
    }

    public OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .ticker(order.getStock().getTicker())
                .companyName(order.getStock().getCompanyName())
                .side(order.getSide())
                .type(order.getType())
                .status(order.getStatus())
                .quantity(order.getQuantity())
                .limitPrice(order.getLimitPrice())
                .executedPrice(order.getExecutedPrice())
                .totalOrderValue(order.getTotalOrderValue())
                .isMarginOrder(order.isMarginOrder())
                .placedAt(order.getPlacedAt())
                .executedAt(order.getExecutedAt())
                .build();
    }

    public List<OrderResponse> getOrdersByUser(User user) {
        // Used by AdminController to view a specific user's orders.
        return orderRepository.findByUserOrderByPlacedAtDesc(user)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }
}