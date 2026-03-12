package com.stockexchange.service;

import com.stockexchange.dto.request.LoginRequest;
import com.stockexchange.dto.request.RegisterRequest;
import com.stockexchange.dto.response.AuthResponse;
import com.stockexchange.entity.MarginAccount;
import com.stockexchange.entity.Portfolio;
import com.stockexchange.entity.User;
import com.stockexchange.enums.Role;
import com.stockexchange.repository.MarginAccountRepository;
import com.stockexchange.repository.PortfolioRepository;
import com.stockexchange.repository.UserRepository;
import com.stockexchange.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PortfolioRepository   portfolioRepository;
    private final MarginAccountRepository marginAccountRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;

    @Value("${margin.default.multiplier}")
    private BigDecimal defaultMarginMultiplier;
    // 5x from application.properties

    // ── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Step 1 — Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken: "
                    + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: "
                    + request.getEmail());
        }

        // Step 2 — Create and save User
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // BCrypt hash — never store plain text.
        user.setRole(Role.ROLE_USER);
        // All self-registered users get ROLE_USER.
        // Admin accounts are created directly in the database.
        user.setBalance(request.getInitialBalance());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // Step 3 — Create Portfolio for the new user
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(savedUser);
        portfolio.setTotalInvestedValue(BigDecimal.ZERO);
        portfolio.setCurrentMarketValue(BigDecimal.ZERO);
        portfolio.setTotalProfitLoss(BigDecimal.ZERO);
        portfolio.setTotalProfitLossPercentage(BigDecimal.ZERO);
        portfolio.setLastUpdatedAt(LocalDateTime.now());
        portfolioRepository.save(portfolio);

        // Step 4 — Create MarginAccount for the new user
        BigDecimal marginLimit = request.getInitialBalance()
                .multiply(defaultMarginMultiplier);
        // margin limit = balance × 5

        MarginAccount marginAccount = new MarginAccount();
        marginAccount.setUser(savedUser);
        marginAccount.setMarginLimit(marginLimit);
        marginAccount.setMarginUsed(BigDecimal.ZERO);
        marginAccount.setMarginAvailable(marginLimit);
        marginAccount.setMarginMultiplier(defaultMarginMultiplier);
        marginAccount.setMarginCallTriggered(false);
        marginAccount.setMarginCallThreshold(
                marginLimit.multiply(new BigDecimal("0.8")));
        marginAccount.setLastUpdatedAt(LocalDateTime.now());
        marginAccountRepository.save(marginAccount);

        // Step 5 — Generate JWT and return response
        String token = jwtTokenProvider.generateToken(savedUser.getUsername());
        log.info("New user registered: {}", savedUser.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .balance(savedUser.getBalance())
                .build();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Step 1 — Authenticate credentials
        // AuthenticationManager calls UserDetailsServiceImpl.loadUserByUsername()
        // then BCrypt.verify(rawPassword, storedHash).
        // Throws BadCredentialsException if wrong — Spring handles it.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Step 2 — Load user and update last login time
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Step 3 — Generate JWT token
        String token = jwtTokenProvider.generateToken(authentication);
        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .balance(user.getBalance())
                .build();
    }
}