
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

    private final JwtTokenProvider        jwtTokenProvider;
    private final UserDetailsServiceImpl  userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        log.info("=== JWT FILTER === URI: {}", request.getRequestURI());
        log.info("=== JWT FILTER === Auth header: {}", request.getHeader("Authorization"));


        try {
            String token = extractTokenFromRequest(request);
            log.info("=== JWT FILTER === Token extracted: {}",
                    token != null ? token.substring(0, 20) + "..." : "NULL");

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                log.info("=== JWT FILTER === Token VALID");

                String username = jwtTokenProvider.extractUsername(token);
                log.info("=== JWT FILTER === Username: {}", username);

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);
                log.info("=== JWT FILTER === UserDetails loaded: {}",
                        userDetails.getUsername());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("=== JWT FILTER === Authentication set successfully");

            } else {
                log.warn("=== JWT FILTER === Token is NULL or INVALID");
            }

        } catch (Exception e) {
            log.error("=== JWT FILTER === Exception type: {}",
                    e.getClass().getName());
            log.error("=== JWT FILTER === Exception message: {}",
                    e.getMessage());
            log.error("=== JWT FILTER === Stack trace: ", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
