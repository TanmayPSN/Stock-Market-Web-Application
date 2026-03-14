package com.stockexchange.service;

import com.stockexchange.dto.response.PortfolioResponse;
import com.stockexchange.entity.*;
import com.stockexchange.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository        portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final UserRepository             userRepository;
    private final MarginAccountRepository    marginAccountRepository;
    private final StockRepository            stockRepository;

    // ── Read Portfolio ───────────────────────────────────────────────────────

    public PortfolioResponse getMyPortfolio() {
        User user = getCurrentUser();
        return buildPortfolioResponse(user);
    }

    public PortfolioResponse getPortfolioByUserId(Long userId) {
        // Used by AdminController to view any user's portfolio.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userId));
        return buildPortfolioResponse(user);
    }

    // ── Update Holdings after Trade ──────────────────────────────────────────

    @Transactional
    // Pass isMarginOrder into the method — add it as a parameter
    public void updateHoldingOnBuy(User user, Stock stock,
                                   int quantity, BigDecimal executedPrice,
                                   boolean isMarginOrder) {
        Portfolio portfolio = getPortfolioEntity(user);

        holdingRepository
                .findByPortfolioAndStock(portfolio, stock)
                .ifPresentOrElse(
                        holding -> {
                            BigDecimal totalCost = holding.getTotalInvestedAmount()
                                    .add(executedPrice.multiply(
                                            BigDecimal.valueOf(quantity)));
                            int newQuantity = holding.getQuantityOwned() + quantity;

                            holding.setQuantityOwned(newQuantity);
                            holding.setTotalInvestedAmount(totalCost);
                            holding.setAverageBuyPrice(
                                    totalCost.divide(
                                            BigDecimal.valueOf(newQuantity),
                                            2, RoundingMode.HALF_UP));
                            holding.setMarginPosition(isMarginOrder); // ← add this
                            holding.setLastUpdatedAt(LocalDateTime.now());
                            holdingRepository.save(holding);
                        },
                        () -> {
                            PortfolioHolding newHolding = new PortfolioHolding();
                            newHolding.setPortfolio(portfolio);
                            newHolding.setStock(stock);
                            newHolding.setQuantityOwned(quantity);
                            newHolding.setAverageBuyPrice(executedPrice);
                            newHolding.setTotalInvestedAmount(
                                    executedPrice.multiply(
                                            BigDecimal.valueOf(quantity)));
                            newHolding.setMarginPosition(isMarginOrder); // ← add this
                            newHolding.setLastUpdatedAt(LocalDateTime.now());
                            holdingRepository.save(newHolding);
                        }
                );

        recalculatePortfolioTotals(portfolio);
    }

    @Transactional
    public void updateHoldingOnSell(User user, Stock stock,
                                    int quantity, BigDecimal executedPrice) {
        // Called by TradeService when a SELL trade executes.
        Portfolio portfolio = getPortfolioEntity(user);

        PortfolioHolding holding = holdingRepository
                .findByPortfolioAndStock(portfolio, stock)
                .orElseThrow(() -> new RuntimeException(
                        "No holding found for stock: " + stock.getTicker()));

        if (holding.getQuantityOwned() < quantity) {
            throw new RuntimeException("Insufficient shares to sell");
        }

        int newQuantity = holding.getQuantityOwned() - quantity;
        holding.setQuantityOwned(newQuantity);

        if (newQuantity == 0) {
            // All shares sold — remove the holding row entirely.
            holdingRepository.delete(holding);
        } else {
            // Average buy price stays the same — only quantity decreases.
            holding.setTotalInvestedAmount(
                    holding.getAverageBuyPrice().multiply(
                            BigDecimal.valueOf(newQuantity)));
            holding.setLastUpdatedAt(LocalDateTime.now());
            holdingRepository.save(holding);
        }

        recalculatePortfolioTotals(portfolio);
    }

    // ── Recalculation ────────────────────────────────────────────────────────

    @Transactional
    public void recalculatePortfolioTotals(Portfolio portfolio) {
        // Recalculates all portfolio summary values from scratch.
        // Called after every trade and every price update.
        List<PortfolioHolding> holdings =
                holdingRepository.findByPortfolio(portfolio);

        BigDecimal totalInvested = holdings.stream()
                .map(PortfolioHolding::getTotalInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentMarketValue = holdings.stream()
                .map(h -> h.getStock().getCurrentPrice()
                        .multiply(BigDecimal.valueOf(h.getQuantityOwned())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitLoss = currentMarketValue.subtract(totalInvested);

        BigDecimal profitLossPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) != 0) {
            profitLossPercent = profitLoss
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        portfolio.setTotalInvestedValue(totalInvested);
        portfolio.setCurrentMarketValue(currentMarketValue);
        portfolio.setTotalProfitLoss(profitLoss);
        portfolio.setTotalProfitLossPercentage(profitLossPercent);
        portfolio.setLastUpdatedAt(LocalDateTime.now());
        portfolioRepository.save(portfolio);
    }

    // ── Response Builder ─────────────────────────────────────────────────────

    private PortfolioResponse buildPortfolioResponse(User user) {
        Portfolio portfolio = getPortfolioEntity(user);
        List<PortfolioHolding> holdings =
                holdingRepository.findByPortfolio(portfolio);

        MarginAccount margin = marginAccountRepository
                .findByUser(user).orElse(null);

        List<PortfolioResponse.HoldingResponse> holdingResponses =
                holdings.stream()
                        .filter(h -> h.getQuantityOwned() > 0)
                        .map(this::toHoldingResponse)
                        .toList();

        BigDecimal totalPortfolioValue = user.getBalance()
                .add(portfolio.getCurrentMarketValue());

        return PortfolioResponse.builder()
                .username(user.getUsername())
                .availableBalance(user.getBalance())
                .totalInvestedValue(portfolio.getTotalInvestedValue())
                .currentMarketValue(portfolio.getCurrentMarketValue())
                .totalProfitLoss(portfolio.getTotalProfitLoss())
                .totalProfitLossPercentage(
                        portfolio.getTotalProfitLossPercentage())
                .totalPortfolioValue(totalPortfolioValue)
                .holdings(holdingResponses)
                .marginEnabled(margin != null)
                .marginUsed(margin != null
                        ? margin.getMarginUsed() : BigDecimal.ZERO)
                .marginAvailable(margin != null
                        ? margin.getMarginAvailable() : BigDecimal.ZERO)
                .marginCallTriggered(margin != null
                        && margin.isMarginCallTriggered())
                .lastUpdatedAt(portfolio.getLastUpdatedAt())
                .build();
    }

    private PortfolioResponse.HoldingResponse toHoldingResponse(
            PortfolioHolding holding) {
        BigDecimal currentPrice = holding.getStock().getCurrentPrice();
        BigDecimal currentValue = currentPrice.multiply(
                BigDecimal.valueOf(holding.getQuantityOwned()));
        BigDecimal profitLoss = currentValue
                .subtract(holding.getTotalInvestedAmount());

        BigDecimal profitLossPercent = BigDecimal.ZERO;
        if (holding.getTotalInvestedAmount()
                .compareTo(BigDecimal.ZERO) != 0) {
            profitLossPercent = profitLoss
                    .divide(holding.getTotalInvestedAmount(),
                            4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        String direction = currentPrice.compareTo(
                holding.getStock().getPreviousClosePrice()) >= 0
                ? "UP" : "DOWN";

        return PortfolioResponse.HoldingResponse.builder()
                .ticker(holding.getStock().getTicker())
                .companyName(holding.getStock().getCompanyName())
                .quantityOwned(holding.getQuantityOwned())
                .averageBuyPrice(holding.getAverageBuyPrice())
                .currentPrice(currentPrice)
                .currentValue(currentValue)
                .investedAmount(holding.getTotalInvestedAmount())
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPercent)
                .priceDirection(direction)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public Portfolio getPortfolioEntity(User user) {
        return portfolioRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Portfolio not found for user: "
                                + user.getUsername()));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));
    }
}