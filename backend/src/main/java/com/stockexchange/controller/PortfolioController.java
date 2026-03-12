package com.stockexchange.controller;

import com.stockexchange.dto.response.PortfolioResponse;
import com.stockexchange.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    // ROLE_USER only — users view their own portfolio.
    // JWT filter extracts the username from the token,
    // PortfolioService.getMyPortfolio() uses SecurityContextHolder
    // to identify which user is requesting.
    public ResponseEntity<PortfolioResponse> getMyPortfolio() {
        return ResponseEntity.ok(portfolioService.getMyPortfolio());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    // Admin can view any user's portfolio by their ID.
    // Used in the admin panel user management section.
    public ResponseEntity<PortfolioResponse> getPortfolioByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                portfolioService.getPortfolioByUserId(userId));
    }
}