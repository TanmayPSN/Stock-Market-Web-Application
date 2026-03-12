package com.stockexchange.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
// Automatically binds application.properties values:
// jwt.secret     → secret field
// jwt.expiration → expiration field
// No need to use @Value on each field individually.
public class JwtConfig {

    private String secret;
    // The base64-encoded secret key used to sign JWT tokens.
    // Read from application.properties: jwt.secret=404E635266...
    // Never log or expose this value.

    private long expiration;
    // Token validity in milliseconds.
    // Read from application.properties: jwt.expiration=86400000
    // Used by JwtTokenProvider when building the token expiry date.
}