package com.stockexchange.service;

import com.stockexchange.dto.request.MarketHoursRequest;
import com.stockexchange.entity.MarketHours;
import com.stockexchange.entity.User;
import com.stockexchange.enums.MarketStatus;
import com.stockexchange.repository.MarketHoursRepository;
import com.stockexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketHoursService {
    // First service in the dependency chain.
    // Every other service that involves orders calls isMarketOpen()
    // before doing anything else.

    private final MarketHoursRepository marketHoursRepository;
    private final UserRepository        userRepository;

    @Value("${market.open.time}")
    private String defaultOpenTime;
    // "09:15" from application.properties

    @Value("${market.close.time}")
    private String defaultCloseTime;
    // "15:30" from application.properties

    @Value("${market.timezone}")
    private String defaultTimezone;
    // "Asia/Kolkata" from application.properties

    // ── Market Status Check ──────────────────────────────────────────────────

    public boolean isMarketOpen() {
        // Called by OrderService before every order placement.
        // Returns true only if current time is within market hours
        // AND admin has not manually closed the market.
        MarketHours marketHours = getActiveMarketHours();

        if (!marketHours.isMarketOpen()) {
            return false;
            // Admin has manually closed the market — reject all orders.
        }

        ZonedDateTime now = ZonedDateTime.now(
                ZoneId.of(marketHours.getTimezone()));
        LocalTime currentTime = now.toLocalTime();

        return !currentTime.isBefore(marketHours.getOpenTime())
                && !currentTime.isAfter(marketHours.getCloseTime());
        // True only if currentTime is between openTime and closeTime.
    }

    public MarketStatus getMarketStatus() {
        // Returns detailed status for the frontend dashboard.
        MarketHours marketHours = getActiveMarketHours();

        if (!marketHours.isMarketOpen()) {
            return MarketStatus.CLOSED;
        }

        ZonedDateTime now = ZonedDateTime.now(
                ZoneId.of(marketHours.getTimezone()));
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.isBefore(marketHours.getOpenTime())) {
            return MarketStatus.PRE_MARKET;
        } else if (currentTime.isAfter(marketHours.getCloseTime())) {
            return MarketStatus.POST_MARKET;
        } else {
            return MarketStatus.OPEN;
        }
    }

    // ── Admin Controls ───────────────────────────────────────────────────────

    @Transactional
    public MarketHours updateMarketHours(MarketHoursRequest request) {
        // Admin updates market open/close times.
        validateMarketHoursRequest(request);

        MarketHours marketHours = getActiveMarketHours();
        User admin = getCurrentAdmin();

        marketHours.setOpenTime(request.getOpenTime());
        marketHours.setCloseTime(request.getCloseTime());
        marketHours.setTimezone(request.getTimezone());
        marketHours.setUpdatedBy(admin);
        marketHours.setLastUpdatedAt(
                java.time.LocalDateTime.now());

        if (request.isForceClose()) {
            marketHours.setMarketOpen(false);
            log.info("Market forcefully closed by admin: {}",
                    admin.getUsername());
        }

        MarketHours saved = marketHoursRepository.save(marketHours);
        log.info("Market hours updated by admin {}: {} - {}",
                admin.getUsername(),
                request.getOpenTime(),
                request.getCloseTime());
        return saved;
    }

    @Transactional
    public MarketHours toggleMarket(boolean open) {
        // Admin manually opens or closes the market.
        MarketHours marketHours = getActiveMarketHours();
        User admin = getCurrentAdmin();

        marketHours.setMarketOpen(open);
        marketHours.setUpdatedBy(admin);
        marketHours.setLastUpdatedAt(java.time.LocalDateTime.now());

        log.info("Market {} by admin: {}",
                open ? "OPENED" : "CLOSED",
                admin.getUsername());
        return marketHoursRepository.save(marketHours);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public MarketHours getActiveMarketHours() {
        // Returns the most recent market hours config.
        // If none exists yet, creates a default one from application.properties.
        return marketHoursRepository.findTopByOrderByLastUpdatedAtDesc()
                .orElseGet(this::createDefaultMarketHours);
    }

    @Transactional
    private MarketHours createDefaultMarketHours() {
        // Runs only on first startup when no config exists in DB.
        MarketHours defaults = new MarketHours();
        defaults.setOpenTime(LocalTime.parse(defaultOpenTime));
        defaults.setCloseTime(LocalTime.parse(defaultCloseTime));
        defaults.setTimezone(defaultTimezone);
        defaults.setMarketOpen(false);
        // Market starts closed — admin must explicitly open it.
        defaults.setLastUpdatedAt(java.time.LocalDateTime.now());
        return marketHoursRepository.save(defaults);
    }

    private void validateMarketHoursRequest(MarketHoursRequest request) {
        if (request.getOpenTime().isAfter(request.getCloseTime())) {
            throw new IllegalArgumentException(
                    "Open time must be before close time");
        }
    }

    private User getCurrentAdmin() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}