package com.stockexchange.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;
    // Chosen username for the new account.
    // @Size enforces min 3 so usernames like "ab" are rejected.
    // AuthService checks this against UserRepository to prevent duplicates.

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    // @Email validates format — "test@" is rejected, "test@gmail.com" is accepted.
    // AuthService checks this against UserRepository to prevent duplicate emails.

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    // Raw password — AuthService will BCrypt encode this before saving.
    // @Size min 6 enforces basic password strength.
    // Never stored in plain text.

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "1000.00", message = "Minimum initial balance is 1000")
    private BigDecimal initialBalance;
    // Starting cash balance for the trading account.
    // @DecimalMin ensures users start with at least 1000
    // so they can actually place trades.
    // Stored in User.balance and Portfolio is initialized with this.
}