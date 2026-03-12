package com.stockexchange.enums;

public enum Role {
    ROLE_ADMIN,
    ROLE_USER
    // Prefixed with ROLE_ because Spring Security expects this convention.
    // When you write @PreAuthorize("hasRole('ADMIN')"), Spring internally
    // looks for ROLE_ADMIN in the user's authorities.
}