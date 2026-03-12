package com.stockexchange.controller;

import com.stockexchange.dto.request.LoginRequest;
import com.stockexchange.dto.request.RegisterRequest;
import com.stockexchange.dto.response.AuthResponse;
import com.stockexchange.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    // Fully public — no JWT required for these endpoints.
    // Permitted in SecurityConfig: .requestMatchers("/api/auth/**").permitAll()

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        // @Valid triggers validation annotations on RegisterRequest —
        // @NotBlank, @Email, @Size, @DecimalMin.
        // If validation fails, Spring returns 400 Bad Request automatically.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        // On success returns JWT token + user details.
        // Frontend stores the token and attaches it to every future request.
        // On failure Spring Security throws BadCredentialsException → 401.
        return ResponseEntity.ok(authService.login(request));
    }
}