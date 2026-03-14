package com.stockexchange.service;

import com.stockexchange.dto.response.TradeResponse;
import com.stockexchange.entity.*;
import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderStatus;
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
public class TradeService {

    private final TradeRepository     tradeRepository;
    private final OrderRepository     orderRepository;
    private final UserRepository      userRepository;
    private final StockRepository     stockRepository;
    private final PortfolioService    portfolioService;
    private final MarginService       marginService;

    // ── Core Trade Execution ─────────────────────────────────────────────────

    @Transactional
    public void executeUserToUserTrade(Order buyOrder, Order sellOrder,
                                       BigDecimal executedPrice) {
        User  buyer  = buyOrder.getUser();
        User  seller = sellOrder.getUser();
        Stock stock  = buyOrder.getStock();

        // Match on the smaller remaining quantity of the two orders
        int buyRemaining  = buyOrder.getQuantity()
                - buyOrder.getFilledQuantity();
        int sellRemaining = sellOrder.getQuantity()
                - sellOrder.getFilledQuantity();
        int qty = Math.min(buyRemaining, sellRemaining);

        BigDecimal tradeValue = executedPrice
                .multiply(BigDecimal.valueOf(qty));

        // ── Buyer side ──
        if (buyOrder.isMarginOrder()) {
            marginService.allocateMargin(buyer, tradeValue);
        } else {
            buyer.setBalance(buyer.getBalance().subtract(tradeValue));
            userRepository.save(buyer);
        }
        portfolioService.updateHoldingOnBuy(
                buyer, stock, qty, executedPrice,
                buyOrder.isMarginOrder());

        // ── Seller side ──
        if (sellOrder.isMarginOrder()) {
            marginService.releaseMargin(seller, tradeValue);
        } else {
            seller.setBalance(seller.getBalance().add(tradeValue));
            userRepository.save(seller);
        }
        portfolioService.updateHoldingOnSell(
                seller, stock, qty, executedPrice);

        // ── Stock available shares — net zero for user-to-user ──
        // Buyer takes qty, seller returns qty — no net change needed.
        // Only update if quantities differ (partial) — still net zero.

        // ── Update filled quantities ──
        buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + qty);
        sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + qty);

        // ── Mark fully filled orders as EXECUTED ──
        // Mark partially filled orders as PARTIAL ──
        updateOrderStatus(buyOrder,  executedPrice, tradeValue);
        updateOrderStatus(sellOrder, executedPrice, tradeValue);

        // ── Trade record ──
        Trade trade = new Trade();
        trade.setBuyOrder(buyOrder);
        trade.setSellOrder(sellOrder);
        trade.setStock(stock);
        trade.setBuyer(buyer);
        trade.setSeller(seller);
        trade.setQuantity(qty);
        trade.setExecutedPrice(executedPrice);
        trade.setTotalTradeValue(tradeValue);
        trade.setExecutedAt(LocalDateTime.now());
        tradeRepository.save(trade);

        log.info("PARTIAL/FULL trade: {} x{} @ {} | {} → {} | BUY filled {}/{} SELL filled {}/{}",
                stock.getTicker(), qty, executedPrice,
                buyer.getUsername(), seller.getUsername(),
                buyOrder.getFilledQuantity(), buyOrder.getQuantity(),
                sellOrder.getFilledQuantity(), sellOrder.getQuantity());
    }

    private void updateOrderStatus(Order order,
                                   BigDecimal executedPrice,
                                   BigDecimal tradeValue) {
        int remaining = order.getQuantity() - order.getFilledQuantity();

        if (remaining == 0) {
            // Fully filled
            order.setStatus(OrderStatus.EXECUTED);
            order.setExecutedPrice(executedPrice);
            order.setTotalOrderValue(
                    executedPrice.multiply(
                            BigDecimal.valueOf(order.getQuantity())));
            order.setExecutedAt(LocalDateTime.now());
        } else {
            // Partially filled — stays PENDING for remaining qty
            order.setStatus(OrderStatus.PARTIAL);
            order.setExecutedPrice(executedPrice);
            // executedPrice reflects last fill price
        }
        orderRepository.save(order);
    }

    @Transactional
    public void executeTrade(Order order, BigDecimal executedPrice) {
        // This is the heart of the system.
        // Called for MARKET orders immediately and LIMIT orders
        // when price condition is met.

        User  user  = order.getUser();
        Stock stock = order.getStock();
        int   qty   = order.getQuantity();
        BigDecimal tradeValue = executedPrice.multiply(
                BigDecimal.valueOf(qty));

        if (order.getSide() == OrderSide.BUY) {
            executeBuyTrade(order, user, stock,
                    qty, executedPrice, tradeValue);
        } else {
            executeSellTrade(order, user, stock,
                    qty, executedPrice, tradeValue);
        }
    }

    // ── Buy Execution ────────────────────────────────────────────────────────

    @Transactional
    protected void executeBuyTrade(Order order, User user, Stock stock,
                                   int qty, BigDecimal executedPrice,
                                   BigDecimal tradeValue) {

        log.info("=== MARGIN DEBUG === isMarginOrder: {}", order.isMarginOrder());
        log.info("=== MARGIN DEBUG === order id: {}", order.getId());

        // Step 1 — Deduct balance or margin
        if (order.isMarginOrder()) {
            marginService.allocateMargin(user, tradeValue);
            // Margin service handles balance + margin split.
        } else {
            user.setBalance(user.getBalance().subtract(tradeValue));
            userRepository.save(user);
        }

        // Step 2 — Reduce available shares in the stock
        stock.setTotalSharesAvailable(
                stock.getTotalSharesAvailable() - qty);
        stockRepository.save(stock);

        // Step 3 — Update portfolio holding
        portfolioService.updateHoldingOnBuy(user, stock, qty, executedPrice, order.isMarginOrder());

        // Step 4 — Update order status
        updateOrderAsExecuted(order, executedPrice, tradeValue);

        // Step 5 — Create trade record
        // For buy orders, the system acts as the seller (exchange).
        Trade trade = buildTrade(order, user, null,
                stock, qty, executedPrice, tradeValue);
        tradeRepository.save(trade);

        log.info("BUY trade executed: {} x{} @ {} for user {}",
                stock.getTicker(), qty, executedPrice,
                user.getUsername());
    }

    // ── Sell Execution ───────────────────────────────────────────────────────

    @Transactional
    protected void executeSellTrade(Order order, User user, Stock stock,
                                    int qty, BigDecimal executedPrice,
                                    BigDecimal tradeValue) {
        // Step 1 — Credit balance
        if (order.isMarginOrder()) {
            marginService.releaseMargin(user, tradeValue);
        } else {
            user.setBalance(user.getBalance().add(tradeValue));
            userRepository.save(user);
        }

        // Step 2 — Increase available shares in the stock
        stock.setTotalSharesAvailable(
                stock.getTotalSharesAvailable() + qty);
        stockRepository.save(stock);

        // Step 3 — Update portfolio holding
        portfolioService.updateHoldingOnSell(
                user, stock, qty, executedPrice);

        // Step 4 — Update order status
        updateOrderAsExecuted(order, executedPrice, tradeValue);

        // Step 5 — Create trade record
        Trade trade = buildTrade(order, null, user,
                stock, qty, executedPrice, tradeValue);
        tradeRepository.save(trade);

        log.info("SELL trade executed: {} x{} @ {} for user {}",
                stock.getTicker(), qty, executedPrice,
                user.getUsername());
    }

    // ── Trade History ────────────────────────────────────────────────────────

    public List<TradeResponse> getMyTradeHistory() {
        User user = getCurrentUser();
        return tradeRepository.findAllTradesByUser(user)
                .stream()
                .map(t -> toTradeResponse(t, user))
                .toList();
    }

    public List<TradeResponse> getAllTrades() {
        return tradeRepository.findAllTradesOrderedByDate()
                .stream()
                .map(t -> toTradeResponseForAdmin(t))
                .toList();
    }

    private TradeResponse toTradeResponseForAdmin(Trade trade) {
        // For admin, derive side from whether buyer or seller is EXCHANGE
        String side = trade.getSeller() == null ? "BUY" : "SELL";

        return TradeResponse.builder()
                .id(trade.getId())
                .ticker(trade.getStock().getTicker())
                .companyName(trade.getStock().getCompanyName())
                .buyerUsername(trade.getBuyer() != null
                        ? trade.getBuyer().getUsername() : "EXCHANGE")
                .sellerUsername(trade.getSeller() != null
                        ? trade.getSeller().getUsername() : "EXCHANGE")
                .quantity(trade.getQuantity())
                .executedPrice(trade.getExecutedPrice())
                .totalTradeValue(trade.getTotalTradeValue())
                .side(side)
                .executedAt(trade.getExecutedAt())
                .build();
    }
    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateOrderAsExecuted(Order order,
                                       BigDecimal executedPrice,
                                       BigDecimal tradeValue) {
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutedPrice(executedPrice);
        order.setTotalOrderValue(tradeValue);
        order.setExecutedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private Trade buildTrade(Order order, User buyer, User seller,
                             Stock stock, int qty,
                             BigDecimal executedPrice,
                             BigDecimal tradeValue) {
        Trade trade = new Trade();
        trade.setBuyOrder(order.getSide() == OrderSide.BUY ? order : null);
        trade.setSellOrder(order.getSide() == OrderSide.SELL ? order : null);
        trade.setStock(stock);
        trade.setBuyer(buyer);
        trade.setSeller(seller);
        trade.setQuantity(qty);
        trade.setExecutedPrice(executedPrice);
        trade.setTotalTradeValue(tradeValue);
        trade.setExecutedAt(LocalDateTime.now());
        return trade;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));
    }

    public TradeResponse toTradeResponse(Trade trade, User requestingUser) {
        String side = "N/A";
        if (requestingUser != null) {
            side = trade.getBuyer() != null
                    && trade.getBuyer().getId()
                    .equals(requestingUser.getId())
                    ? "BUY" : "SELL";
        }

        return TradeResponse.builder()
                .id(trade.getId())
                .ticker(trade.getStock().getTicker())
                .companyName(trade.getStock().getCompanyName())
                .buyerUsername(trade.getBuyer() != null
                        ? trade.getBuyer().getUsername() : "EXCHANGE")
                .sellerUsername(trade.getSeller() != null
                        ? trade.getSeller().getUsername() : "EXCHANGE")
                .quantity(trade.getQuantity())
                .executedPrice(trade.getExecutedPrice())
                .totalTradeValue(trade.getTotalTradeValue())
                .side(side)
                .executedAt(trade.getExecutedAt())
                .build();
    }
}