package com.stockexchange.repository;

import com.stockexchange.entity.User;
import com.stockexchange.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    // Used by UserDetailsServiceImpl to load user during JWT authentication.
    // Returns Optional so we can handle user-not-found cleanly.

    Optional<User> findByEmail(String email);
    // Used during registration to check if email is already taken.

    boolean existsByUsername(String username);
    // Used during registration to reject duplicate usernames
    // without fetching the full User object.

    boolean existsByEmail(String email);
    // Used during registration to reject duplicate emails.

    List<User> findByRole(Role role);
    // Used by AdminController to list all ROLE_USER accounts.

    List<User> findByIsActiveTrue();
    // Used by AdminController to see all currently active users.

    List<User> findByIsActiveFalse();
    // Used by AdminController to see all deactivated accounts.
}