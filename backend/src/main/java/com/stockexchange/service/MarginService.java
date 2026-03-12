package com.stockexchange.service;

import com.stockexchange.entity.MarginAccount;
import com.stockexchange.entity.User;
import com.stockexchange.repository.MarginAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarginService {

    private final MarginAccountRepository marginAccountRepository;

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
        // Called when a margin BUY order executes.
        MarginAccount margin = getMarginAccount(user);

        BigDecimal userBalanceContribution =
                user.getBalance().min(tradeValue);
        // User's own balance covers as much as possible first.

        BigDecimal marginRequired =
                tradeValue.subtract(userBalanceContribution);
        // Remainder is taken from margin.

        if (marginRequired.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newMarginUsed =
                    margin.getMarginUsed().add(marginRequired);
            BigDecimal newMarginAvailable =
                    margin.getMarginLimit().subtract(newMarginUsed);

            margin.setMarginUsed(newMarginUsed);
            margin.setMarginAvailable(newMarginAvailable);
            margin.setLastUpdatedAt(LocalDateTime.now());

            checkAndTriggerMarginCall(margin);
            marginAccountRepository.save(margin);

            log.info("Margin allocated for user {}: {}",
                    user.getUsername(), marginRequired);
        }
    }

    @Transactional
    public void releaseMargin(User user, BigDecimal tradeValue) {
        // Called when a margin SELL order executes — releases borrowed margin.
        MarginAccount margin = getMarginAccount(user);

        BigDecimal releaseAmount =
                tradeValue.min(margin.getMarginUsed());
        // Cannot release more than what was borrowed.

        BigDecimal newMarginUsed =
                margin.getMarginUsed().subtract(releaseAmount);
        BigDecimal newMarginAvailable =
                margin.getMarginLimit().subtract(newMarginUsed);

        margin.setMarginUsed(newMarginUsed);
        margin.setMarginAvailable(newMarginAvailable);
        margin.setLastUpdatedAt(LocalDateTime.now());

        // Check if margin call should be cleared.
        if (margin.isMarginCallTriggered()
                && newMarginUsed.compareTo(
                margin.getMarginCallThreshold()) < 0) {
            margin.setMarginCallTriggered(false);
            log.info("Margin call cleared for user: {}",
                    user.getUsername());
        }

        marginAccountRepository.save(margin);
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
        return marginAccountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Margin account not found for user: "
                                + user.getUsername()));
    }
}