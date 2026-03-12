package com.stockexchange.controller;

import com.stockexchange.dto.response.OrderResponse;
import com.stockexchange.dto.response.PortfolioResponse;
import com.stockexchange.dto.response.TradeResponse;
import com.stockexchange.entity.MarginAccount;
import com.stockexchange.entity.User;
import com.stockexchange.repository.MarginAccountRepository;
import com.stockexchange.repository.UserRepository;
import com.stockexchange.service.OrderService;
import com.stockexchange.service.PortfolioService;
import com.stockexchange.service.StockService;
import com.stockexchange.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
// Class-level @PreAuthorize — every method in this controller
// requires ROLE_ADMIN without needing to annotate each method.
// SecurityConfig also has .requestMatchers("/api/admin/**").hasRole("ADMIN")
// as a second layer of protection — defense in depth.
public class AdminController {

    private final UserRepository          userRepository;
    private final MarginAccountRepository marginAccountRepository;
    private final PortfolioService        portfolioService;
    private final TradeService            tradeService;
    private final OrderService            orderService;
    private final StockService            stockService;

    // ── User Management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        // Returns all registered users.
        // Admin uses this to see the full user list in the admin panel.
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        return ResponseEntity.ok(
                userRepository.findByIsActiveTrue());
    }

    @GetMapping("/users/inactive")
    public ResponseEntity<List<User>> getInactiveUsers() {
        return ResponseEntity.ok(
                userRepository.findByIsActiveFalse());
    }

    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable Long userId) {
        // Admin deactivates a user — they can no longer log in
        // or place orders. Data is preserved.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userId));
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "User " + user.getUsername()
                        + " deactivated successfully"));
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable Long userId) {
        // Admin reactivates a previously deactivated user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userId));
        user.setActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "User " + user.getUsername()
                        + " activated successfully"));
    }

    @GetMapping("/users/{userId}/portfolio")
    public ResponseEntity<PortfolioResponse> getUserPortfolio(
            @PathVariable Long userId) {
        // Admin views a specific user's portfolio.
        return ResponseEntity.ok(
                portfolioService.getPortfolioByUserId(userId));
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable Long userId) {
        // Admin views all orders placed by a specific user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userId));
        return ResponseEntity.ok(
                orderService.getOrdersByUser(user));
    }

    // ── Margin Management ────────────────────────────────────────────────────

    @GetMapping("/margin/calls")
    public ResponseEntity<List<MarginAccount>> getMarginCallAccounts() {
        // Returns all users currently under a margin call.
        // Admin monitors this for risk management.
        return ResponseEntity.ok(
                marginAccountRepository.findAllMarginCallAccounts());
    }

    @GetMapping("/margin/active")
    public ResponseEntity<List<MarginAccount>> getActiveMarginUsers() {
        // Returns all users currently using margin.
        return ResponseEntity.ok(
                marginAccountRepository.findAllActiveMarginUsers());
    }

    @PutMapping("/margin/{userId}/resolve")
    public ResponseEntity<Map<String, String>> resolveMarginCall(
            @PathVariable Long userId) {
        // Admin manually clears a margin call after user adds funds.
        MarginAccount margin = marginAccountRepository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Margin account not found for user: " + userId));
        margin.setMarginCallTriggered(false);
        marginAccountRepository.save(margin);
        return ResponseEntity.ok(Map.of(
                "message", "Margin call resolved for user: " + userId));
    }

    // ── Platform Overview ────────────────────────────────────────────────────

    @GetMapping("/trades")
    public ResponseEntity<List<TradeResponse>> getAllTrades() {
        // Full platform trade history — all users, all stocks.
        return ResponseEntity.ok(tradeService.getAllTrades());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPlatformSummary() {
        // High-level platform stats shown on the admin dashboard.
        long totalUsers    = userRepository.count();
        long activeUsers   = userRepository.findByIsActiveTrue().size();
        long totalTrades   = tradeService.getAllTrades().size();
        long marginCalls   = marginAccountRepository
                .findAllMarginCallAccounts().size();
        long activeStocks  = stockService.getAllActiveStocks().size();

        return ResponseEntity.ok(Map.of(
                "totalUsers",       totalUsers,
                "activeUsers",      activeUsers,
                "totalTrades",      totalTrades,
                "activeMarginCalls", marginCalls,
                "activeStocks",     activeStocks
        ));
    }
}