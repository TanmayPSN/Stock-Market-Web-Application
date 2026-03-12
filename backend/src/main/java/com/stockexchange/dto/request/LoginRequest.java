package com.stockexchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;
    // The username the user enters on the login form.
    // @NotBlank rejects null, empty string, and whitespace-only strings.
    // Validated before AuthService is even called — bad requests
    // are rejected at the controller level immediately.

    @NotBlank(message = "Password is required")
    private String password;
    // Raw plain text password from the login form.
    // AuthService passes this to AuthenticationManager which
    // uses BCrypt to compare it against the stored hash.
    // Never stored or logged anywhere.
}