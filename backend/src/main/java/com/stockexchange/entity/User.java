package com.stockexchange.entity;

import com.stockexchange.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Auto-incremented primary key. Every user gets a unique number.
    // Used by every other table to reference who owns what.

    @Column(nullable = false, unique = true)
    private String username;
    // The login name. unique = true means no two users can have the same username.
    // Used during login to identify the user.

    @Column(nullable = false, unique = true)
    private String email;
    // Email address. Also unique — prevents duplicate accounts.
    // Can be used for password recovery later.

    @Column(nullable = false)
    private String password;
    // Stored as a BCrypt hash — never plain text.
    // BCrypt turns "mypassword123" into "$2a$10$xK8..." so even if
    // the database leaks, passwords are safe.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    // Either ROLE_ADMIN or ROLE_USER.
    // Spring Security reads this to decide what endpoints the user can access.
    // Stored as a string ("ROLE_USER") not a number so it's readable in the DB.

    @Column(nullable = false)
    private BigDecimal balance;
    // The user's cash balance in their trading account.
    // BigDecimal instead of double — financial calculations must be exact.
    // double has floating point errors (0.1 + 0.2 = 0.30000000000000004).

    @Column(nullable = false)
    private boolean isActive;
    // Admin can deactivate a user without deleting their data.
    // If false, the user cannot log in or place orders.

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // Timestamp when the account was created.
    // updatable = false means once set, it never changes.

    private LocalDateTime lastLoginAt;
    // Updated every time the user successfully logs in.
    // Useful for admin to see inactive accounts.
}