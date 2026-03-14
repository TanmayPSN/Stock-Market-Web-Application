package com.stockexchange.service;

import com.stockexchange.entity.MarginAccount;
import com.stockexchange.entity.PortfolioHolding;
import com.stockexchange.entity.User;
import com.stockexchange.repository.MarginAccountRepository;
import com.stockexchange.repository.PortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarginService {

    private final MarginAccountRepository marginAccountRepository;
    private final PortfolioHoldingRepository holdingRepository;

    @Value("${margin.call.threshold}")
    private BigDecimal marginCallThreshold;
    // 0.8 from application.properties — 80% usage triggers margin call.

    // ── Validation ───────────────────────────────────────────────────────────

    public boolean hasSufficientMargin(User user, BigDecimal requiredAmount) {
        // Called by OrderService before placing a margin order.
        MarginAccount margin = getMarginAccount(user);

        if (margin.isMarginCallTriggered()) {
            return false;
            // User under margin call cannot place new trades.
        }

        BigDecimal totalAvailable = user.getBalance()
                .add(margin.getMarginAvailable());
        return totalAvailable.compareTo(requiredAmount) >= 0;
    }

    // ── Update on Trade ──────────────────────────────────────────────────────

    @Transactional
    public void allocateMargin(User user, BigDecimal tradeValue) {

        log.info("=== MARGIN DEBUG === allocateMargin called for user: {}", user.getUsername());
        log.info("=== MARGIN DEBUG === tradeValue: {}", tradeValue);
        log.info("=== MARGIN DEBUG === user balance: {}", user.getBalance());

        MarginAccount margin = getMarginAccount(user);

        // When user explicitly uses margin, always charge from margin first.
        // Do NOT use user's own balance at all for margin orders.
        BigDecimal newMarginUsed = margin.getMarginUsed().add(tradeValue);
        BigDecimal newMarginAvailable = margin.getMarginLimit().subtract(newMarginUsed);

        margin.setMarginUsed(newMarginUsed);
        margin.setMarginAvailable(newMarginAvailable);
        margin.setLastUpdatedAt(LocalDateTime.now());

        checkAndTriggerMarginCall(margin);
        marginAccountRepository.save(margin);

        log.info("Margin allocated for user {}: {}", user.getUsername(), tradeValue);
    }

    @Transactional
    public void releaseMargin(User user, BigDecimal tradeValue) {
        MarginAccount margin = getMarginAccount(user);

        BigDecimal releaseAmount = tradeValue.min(margin.getMarginUsed());
        // Cannot release more than what was borrowed.

        BigDecimal newMarginUsed = margin.getMarginUsed().subtract(releaseAmount);
        BigDecimal newMarginAvailable = margin.getMarginLimit().subtract(newMarginUsed);

        margin.setMarginUsed(newMarginUsed);
        margin.setMarginAvailable(newMarginAvailable);
        margin.setLastUpdatedAt(LocalDateTime.now());

        if (margin.isMarginCallTriggered()
                && newMarginUsed.compareTo(margin.getMarginCallThreshold()) < 0) {
            margin.setMarginCallTriggered(false);
            log.info("Margin call cleared for user: {}", user.getUsername());
        }

        marginAccountRepository.save(margin);
        log.info("Margin released for user {}: {}", user.getUsername(), releaseAmount);
    }

    @Transactional
    public void checkMarginCallsAfterPriceUpdate() {
        // Called by StockPriceSimulator after every price tick.
        // Loops through all active margin accounts and checks if
        // unrealized losses have exceeded the margin call threshold.

        List<MarginAccount> activeMarginAccounts =
                marginAccountRepository.findAllActiveMarginUsers();

        activeMarginAccounts.forEach(margin -> {
            if (margin.isMarginCallTriggered()) return;
            // Already under margin call — skip.

            User user = margin.getUser();

            // Get all margin holdings for this user
            List<PortfolioHolding> holdings = holdingRepository
                    .findByPortfolioUserAndMarginPositionTrue(user);

            if (holdings.isEmpty()) return;

            // Calculate total current value of margin positions
            BigDecimal currentValue = holdings.stream()
                    .map(h -> h.getStock().getCurrentPrice()
                            .multiply(BigDecimal.valueOf(h.getQuantityOwned())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate total invested value of margin positions
            BigDecimal investedValue = holdings.stream()
                    .map(PortfolioHolding::getTotalInvestedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Unrealized loss (positive number means loss)
            BigDecimal unrealizedLoss = investedValue.subtract(currentValue);

            if (unrealizedLoss.compareTo(BigDecimal.ZERO) <= 0) return;
            // No loss — no margin call needed.

            // Trigger if loss >= marginCallThreshold
            if (unrealizedLoss.compareTo(margin.getMarginCallThreshold()) >= 0
                    && !margin.isMarginCallTriggered()) {
                margin.setMarginCallTriggered(true);
                margin.setLastUpdatedAt(LocalDateTime.now());
                marginAccountRepository.save(margin);
                log.warn("MARGIN CALL TRIGGERED (price drop) for user: {}  " +
                                "unrealizedLoss={} threshold={}",
                        user.getUsername(), unrealizedLoss,
                        margin.getMarginCallThreshold());
            }
        });
    }

    // ── Margin Call ──────────────────────────────────────────────────────────

    private void checkAndTriggerMarginCall(MarginAccount margin) {
        // Triggers margin call when marginUsed exceeds 80% of marginLimit.
        BigDecimal usagePercent = margin.getMarginUsed()
                .divide(margin.getMarginLimit(), 4, RoundingMode.HALF_UP);

        if (usagePercent.compareTo(marginCallThreshold) >= 0
                && !margin.isMarginCallTriggered()) {
            margin.setMarginCallTriggered(true);
            log.warn("MARGIN CALL TRIGGERED for user: {}",
                    margin.getUser().getUsername());
            // In a real system, this would also send an email/SMS alert.
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public MarginAccount getMarginAccount(User user) {
        return marginAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Margin account not found for user: "
                                + user.getUsername()));
    }
}