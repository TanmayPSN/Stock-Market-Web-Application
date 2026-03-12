package com.stockexchange.controller;

import com.stockexchange.dto.response.TradeResponse;
import com.stockexchange.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    // Returns complete trade history for the logged-in user —
    // both BUY and SELL sides.
    // Used for the trade history section on the portfolio page.
    public ResponseEntity<List<TradeResponse>> getMyTradeHistory() {
        return ResponseEntity.ok(tradeService.getMyTradeHistory());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    // Admin views all trades across all users on the platform.
    // Used in the admin panel for platform-wide trade monitoring.
    public ResponseEntity<List<TradeResponse>> getAllTrades() {
        return ResponseEntity.ok(tradeService.getAllTrades());
    }
}