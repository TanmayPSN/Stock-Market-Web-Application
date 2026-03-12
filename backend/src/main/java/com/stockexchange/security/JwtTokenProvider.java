package com.stockexchange.security;

import com.stockexchange.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    // Responsible for 3 things:
    // 1. Generating a JWT token after successful login
    // 2. Extracting the username from a token on every request
    // 3. Validating that a token is genuine and not expired

    private final JwtConfig jwtConfig;

    // ── 1. Generate Token ────────────────────────────────────────────────────

    public String generateToken(Authentication authentication) {
        // Called by AuthService after login credentials are verified.
        // authentication.getName() returns the username.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername());
    }

    public String generateToken(String username) {
        // Overload — used when we already have the username directly.
        return buildToken(username);
    }

    private String buildToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpiration());
        // expiry = current time + 86400000ms (24 hours from application.properties)

        return Jwts.builder()
                .subject(username)
                // subject is the main identifier stored inside the token.
                // When the token arrives on the next request, we extract
                // this to know which user is making the request.

                .issuedAt(now)
                // Records when the token was created.

                .expiration(expiry)
                // Token becomes invalid after this time.
                // JwtAuthenticationFilter checks this on every request.

                .signWith(getSigningKey())
                // Signs the token with our secret key.
                // If anyone tampers with the token payload,
                // the signature verification will fail.

                .compact();
        // Produces the final token string:
        // "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSJ9.signature"
        //  ↑ header                ↑ payload              ↑ signature
    }

    // ── 2. Extract Username ──────────────────────────────────────────────────

    public String extractUsername(String token) {
        // Called by JwtAuthenticationFilter on every incoming request.
        // Extracts the username from the token's subject claim.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ── 3. Validate Token ────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        // Returns true if token is genuine and not expired.
        // Returns false and logs the reason if anything is wrong.
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token empty or null: {}", e.getMessage());
        }
        return false;
    }

    // ── Signing Key ──────────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        // Decodes the hex secret from application.properties into a
        // cryptographic key suitable for HMAC-SHA256 signing.
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}