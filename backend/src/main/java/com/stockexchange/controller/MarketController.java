package com.stockexchange.controller;

import com.stockexchange.dto.request.MarketHoursRequest;
import com.stockexchange.entity.MarketHours;
import com.stockexchange.enums.MarketStatus;
import com.stockexchange.service.MarketHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketHoursService marketHoursService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMarketStatus() {
        // Public endpoint — no JWT needed.
        // Frontend dashboard calls this on load to show OPEN/CLOSED banner.
        MarketStatus status = marketHoursService.getMarketStatus();
        MarketHours  hours  = marketHoursService.getActiveMarketHours();

        return ResponseEntity.ok(Map.of(
                "status",    status,
                "openTime",  hours.getOpenTime(),
                "closeTime", hours.getCloseTime(),
                "timezone",  hours.getTimezone(),
                "isOpen",    marketHoursService.isMarketOpen()
        ));
    }

    @PutMapping("/hours")
    @PreAuthorize("hasRole('ADMIN')")
    // Only ROLE_ADMIN can update market hours.
    // @PreAuthorize works because @EnableMethodSecurity is in SecurityConfig.
    public ResponseEntity<MarketHours> updateMarketHours(
            @Valid @RequestBody MarketHoursRequest request) {
        return ResponseEntity.ok(
                marketHoursService.updateMarketHours(request));
    }

    @PutMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> openMarket() {
        // Admin manually opens the market.
        marketHoursService.toggleMarket(true);
        return ResponseEntity.ok(
                Map.of("message", "Market opened successfully"));
    }

    @PutMapping("/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> closeMarket() {
        // Admin manually closes the market.
        // OrderService.cancelAllPendingOrdersAtMarketClose() is
        // triggered inside MarketHoursService.toggleMarket(false).
        marketHoursService.toggleMarket(false);
        return ResponseEntity.ok(
                Map.of("message", "Market closed successfully"));
    }
}