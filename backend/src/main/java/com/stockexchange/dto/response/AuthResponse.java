package com.stockexchange.dto.response;

import com.stockexchange.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;
    // The JWT token string generated after successful login or register.
    // Frontend stores this in localStorage or a context variable.
    // Sent back in every subsequent request as:
    // Authorization: Bearer <token>

    private String username;
    // Returned so the frontend can display "Welcome, username"
    // without making a separate API call to fetch user details.

    private String email;
    // Returned for display purposes on the profile or settings page.

    private Role role;
    // ROLE_ADMIN or ROLE_USER.
    // Frontend uses this to decide which pages to show —
    // admin panel is only visible if role = ROLE_ADMIN.
    // Never trust this alone for security — backend always
    // re-checks role via JWT on every request.

    private BigDecimal balance;
    // Current account balance returned on login so the
    // dashboard can immediately display it without an extra API call.
}