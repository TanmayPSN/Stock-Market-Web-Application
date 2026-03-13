package com.stockexchange.config;

import com.stockexchange.security.JwtAuthenticationFilter;
import com.stockexchange.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
// @EnableMethodSecurity enables @PreAuthorize on controller methods.
// Without this, @PreAuthorize("hasRole('ADMIN')") does nothing.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl  userDetailsService;

    @Value("${frontend.url}")
    private String frontendUrl;
    // http://localhost:5173 — read from application.properties.

    // ── Security Filter Chain ────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CSRF disabled because we use JWT — CSRF protection is only
                // needed for session-cookie based authentication.

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Apply our CORS config so React frontend can call the API.

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // No sessions — every request must carry a JWT.
                // Spring will never create or use an HttpSession.

                .authorizeHttpRequests(auth -> auth

                                // ── Allow ALL preflight OPTIONS requests ──
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // ── Public endpoints — no token required ──
                                .requestMatchers("/api/auth/**").permitAll()
                                // Login and register endpoints are open to everyone.

                                .requestMatchers("/api/market/status").permitAll()
                                // Anyone can check if market is open without logging in.

                                .requestMatchers("/api/stocks/all").permitAll()
                                // Anyone can view stock prices on the public dashboard.

                                .requestMatchers("/ws/**").permitAll()
                                // WebSocket handshake endpoint must be public.

                                // ── Admin only endpoints ──
                                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                                // All /api/admin/** routes require ROLE_ADMIN.
                                // hasRole("ADMIN") automatically checks for "ROLE_ADMIN".

                                // ── Authenticated user endpoints ──
                                .anyRequest().authenticated()
                        // Everything else requires a valid JWT token.
                )

                .authenticationProvider(authenticationProvider())
                // Register our custom DaoAuthenticationProvider which uses
                // UserDetailsServiceImpl and BCrypt password encoder.

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        // Insert our JWT filter before Spring's default login filter.
        // This ensures JWT is checked before any other auth mechanism.

        return http.build();
    }

    // ── CORS Configuration ───────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(frontendUrl));
        // Only allow requests from our React frontend (http://localhost:5173).

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow standard REST methods plus OPTIONS for preflight requests.

        config.setAllowedHeaders(List.of("*"));
        // Allow all headers — includes Authorization header carrying the JWT.

        config.setAllowCredentials(true);
        // Required when frontend sends cookies or Authorization headers.

        config.setMaxAge(3600L); // ← add this — caches preflight for 1 hou

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // Apply this CORS config to every endpoint.

        return source;
    }

    // ── Authentication Provider ──────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        // Tells Spring to use our UserDetailsServiceImpl
        // to load users from the database.

        provider.setPasswordEncoder(passwordEncoder());
        // Tells Spring to use BCrypt when verifying passwords.
        // During login: BCrypt.verify(rawPassword, storedHash).

        return provider;
    }

    // ── Password Encoder ─────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // BCrypt automatically handles salting and hashing.
        // Never store plain text passwords.
        // encode("password") → "$2a$10$randomsalt...hashedvalue"
    }

    // ── Authentication Manager ───────────────────────────────────────────────

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
        // Used by AuthService to trigger Spring Security's authentication process.
        // authenticationManager.authenticate(usernamePasswordToken)
        // internally calls UserDetailsServiceImpl.loadUserByUsername()
        // and verifies the password with BCrypt.
    }
}