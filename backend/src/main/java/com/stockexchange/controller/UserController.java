package com.stockexchange.controller;

import com.stockexchange.entity.User;
import com.stockexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PutMapping("/balance/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> addBalance(
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        User user = getCurrentUser();
        BigDecimal newBalance = user.getBalance().add(amount);
        user.setBalance(newBalance);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message",    "₹" + amount + " added successfully",
                "newBalance", newBalance
        ));
    }

    @PutMapping("/balance/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> withdrawBalance(
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        User user = getCurrentUser();
        if (user.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                    "Insufficient balance. Available: ₹" + user.getBalance());
        }

        BigDecimal newBalance = user.getBalance().subtract(amount);
        user.setBalance(newBalance);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message",    "₹" + amount + " withdrawn successfully",
                "newBalance", newBalance
        ));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));
    }
}