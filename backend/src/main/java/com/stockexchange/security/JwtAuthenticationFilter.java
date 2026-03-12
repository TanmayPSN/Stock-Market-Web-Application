package com.stockexchange.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter guarantees this filter runs exactly once per request.
    // Every HTTP request passes through here before reaching any controller.
    // Job: read the JWT from the Authorization header, validate it,
    //      and set the authenticated user in Spring Security's context.

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            // Step 1 — Extract token from header
            String token = extractTokenFromRequest(request);
            // Reads "Authorization: Bearer eyJhbGci..." header
            // and strips the "Bearer " prefix to get the raw token.

            // Step 2 — Validate token and set authentication
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                String username = jwtTokenProvider.extractUsername(token);
                // Get username from inside the token.

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);
                // Load full user details including role (ROLE_USER / ROLE_ADMIN).

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                                // Authorities contain the role —
                                // Spring uses this for @PreAuthorize checks.
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                // Attaches request metadata (IP address, session) to the auth object.

                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Places the authenticated user into the security context.
                // From this point, any controller can call
                // SecurityContextHolder.getContext().getAuthentication()
                // to get the current logged-in user.
            }

        } catch (Exception e) {
            log.error("Could not set user authentication in security context: {}",
                    e.getMessage());
            // Don't throw — let the filter chain continue.
            // SecurityConfig will reject the request if authentication is missing.
        }

        filterChain.doFilter(request, response);
        // Always pass the request to the next filter / controller
        // regardless of whether authentication succeeded.
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // Frontend sends: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
            // Strip "Bearer " (7 characters) to get the raw JWT string.
        }
        return null;
    }
}