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
    private final MarginAccountRepository    marginAccountRepository;
    private final MarketHoursService         marketHoursService;
    private final MatchingEngineService      matchingEngine;
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

        if (!marketHoursService.isMarketOpen()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Market is closed");
        }

        if (!stock.isActive()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Stock is delisted");
        }

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

        if (request.getType() == OrderType.LIMIT
                && request.getLimitPrice() == null) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Limit price required for limit orders");
        }

        if (stock.getTotalSharesAvailable() < request.getQuantity()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Insufficient shares available");
        }

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

        // ── OPTION 1: Deduct full amount upfront for LIMIT orders ──
        if (request.getType() == OrderType.LIMIT) {
            if (request.isUseMargin()) {
                marginService.allocateMargin(user, totalCost);
            } else {
                user.setBalance(user.getBalance().subtract(totalCost));
                userRepository.save(user);
            }
            log.info("Upfront deduction of {} for LIMIT BUY order by {}",
                    totalCost, user.getUsername());
        }

        Order order = buildOrder(user, stock, request);
        Order savedOrder = orderRepository.save(order);

        if (request.getType() == OrderType.MARKET) {
            matchingEngine.tryMatchOrder(savedOrder, stock.getCurrentPrice());

            Order reloaded = orderRepository.findById(savedOrder.getId())
                    .orElseThrow();
            int remaining = reloaded.getQuantity()
                    - reloaded.getFilledQuantity();

            if (remaining > 0) {
                tradeService.executeTrade(reloaded, stock.getCurrentPrice());
            }
        }

        log.info("Order placed: {} {} {} x{} by {}",
                request.getSide(), request.getType(),
                stock.getTicker(), request.getQuantity(),
                user.getUsername());

        return toOrderResponse(orderRepository.findById(
                savedOrder.getId()).orElse(savedOrder));
    }

    // ── Sell Order ───────────────────────────────────────────────────────────

    private OrderResponse processSellOrder(User user, Stock stock,
                                           PlaceOrderRequest request) {
        if (request.getType() == OrderType.LIMIT
                && request.getLimitPrice() == null) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Limit price required for limit orders");
        }

        PortfolioHolding holding = holdingRepository
                .findByPortfolioIdAndStockId(
                        getUserPortfolioId(user), stock.getId())
                .orElse(null);

        if (holding == null || getOwnedQuantity(user, stock)
                < request.getQuantity()) {
            return rejectOrder(user, stock, request,
                    "Order Rejected: Insufficient shares to sell");
        }

        // Enforce funding source from original buy
        request.setUseMargin(holding.isMarginPosition());

        Order order = buildOrder(user, stock, request);
        Order savedOrder = orderRepository.save(order);

        if (request.getType() == OrderType.MARKET) {
            // Step 1 — try user-to-user matching first
            matchingEngine.tryMatchOrder(savedOrder, stock.getCurrentPrice());

            // Step 2 — reload to check remaining quantity after matching
            Order reloaded = orderRepository.findById(savedOrder.getId())
                    .orElseThrow();
            int remaining = reloaded.getQuantity()
                    - reloaded.getFilledQuantity();

            // Step 3 — exchange fills whatever is left
            if (remaining > 0) {
                tradeService.executeTrade(reloaded, stock.getCurrentPrice());
            }
        }

        return toOrderResponse(orderRepository.findById(
                savedOrder.getId()).orElse(savedOrder));
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
        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.PARTIAL) {
            throw new RuntimeException(
                    "Only PENDING or PARTIAL orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toOrderResponse(orderRepository.save(order));
    }

    // ── Limit Order Execution Check ──────────────────────────────────────────

    @Transactional
    public void checkAndExecuteLimitOrders(Long stockId,
                                           BigDecimal currentPrice) {
        // BUY limit orders — execute when price drops to limit price
        orderRepository
                .findEligibleBuyLimitOrders(stockId, currentPrice)
                .forEach(order -> {
                    matchingEngine.tryMatchOrder(order, currentPrice);
                    Order reloaded = orderRepository
                            .findById(order.getId()).orElseThrow();
                    int remaining = reloaded.getQuantity()
                            - reloaded.getFilledQuantity();
                    if (remaining > 0) {
                        tradeService.executeTrade(reloaded, currentPrice);
                    }
                });

        // SELL limit orders — execute when price rises to limit price
        orderRepository
                .findEligibleSellLimitOrders(stockId, currentPrice)
                .forEach(order -> {
                    matchingEngine.tryMatchOrder(order, currentPrice);
                    Order reloaded = orderRepository
                            .findById(order.getId()).orElseThrow();
                    int remaining = reloaded.getQuantity()
                            - reloaded.getFilledQuantity();
                    if (remaining > 0) {
                        tradeService.executeTrade(reloaded, currentPrice);
                    }
                });
    }

    // ── Market Close — Cancel All Pending and Refund ────────────────────────────────────

    @Transactional
    public void cancelAllPendingOrdersAtMarketClose() {
        List<Order> pendingOrders = orderRepository.findAllPendingOrders();

        pendingOrders.forEach(order -> {
            int remaining = order.getQuantity() - order.getFilledQuantity();

            // Only BUY orders need a refund — SELL orders haven't
            // had their shares removed yet (still in portfolio)
            if (remaining > 0 && order.getSide() == OrderSide.BUY) {
                BigDecimal refundAmount = order.getLimitPrice()
                        .multiply(BigDecimal.valueOf(remaining));

                User user = order.getUser();

                if (order.isMarginOrder()) {
                    MarginAccount margin = marginService.getMarginAccount(user);
                    margin.setMarginUsed(
                            margin.getMarginUsed().subtract(refundAmount));
                    margin.setMarginAvailable(
                            margin.getMarginAvailable().add(refundAmount));
                    margin.setLastUpdatedAt(LocalDateTime.now());
                    marginAccountRepository.save(margin);
                    log.info("Margin refunded {} to user {} for cancelled order {}",
                            refundAmount, user.getUsername(), order.getId());
                } else {
                    user.setBalance(user.getBalance().add(refundAmount));
                    userRepository.save(user);
                    log.info("Balance refunded {} to user {} for cancelled order {}",
                            refundAmount, user.getUsername(), order.getId());
                }
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });

        log.info("Cancelled {} pending/partial orders at market close",
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
        order.setFilledQuantity(0);
        order.setStatus(OrderStatus.PENDING);
        order.setPlacedAt(LocalDateTime.now());
        return order;
    }

    private OrderResponse rejectOrder(User user, Stock stock,
                                      PlaceOrderRequest request,
                                      String reason) {
        Order order = buildOrder(user, stock, request);
        order.setStatus(OrderStatus.REJECTED);
        Order saved = orderRepository.save(order);
        log.warn("Order rejected for user {}: {}",
                user.getUsername(), reason);
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
                .filledQuantity(order.getFilledQuantity())
                .limitPrice(order.getLimitPrice())
                .executedPrice(order.getExecutedPrice())
                .totalOrderValue(order.getTotalOrderValue())
                .isMarginOrder(order.isMarginOrder())
                .placedAt(order.getPlacedAt())
                .executedAt(order.getExecutedAt())
                .build();
    }

    public List<OrderResponse> getOrdersByUser(User user) {
        return orderRepository.findByUserOrderByPlacedAtDesc(user)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }
}