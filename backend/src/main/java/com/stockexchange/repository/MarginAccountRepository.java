package com.stockexchange.repository;

import com.stockexchange.entity.MarginAccount;
import com.stockexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarginAccountRepository extends JpaRepository<MarginAccount, Long> {

    Optional<MarginAccount> findByUser(User user);
    // Primary lookup for a user's margin account.
    // Used by MarginService on every margin trade.

    Optional<MarginAccount> findByUserId(Long userId);
    // Same as above using userId directly —
    // avoids loading the full User object.

    boolean existsByUser(User user);
    // Used during user registration to check if margin
    // account has already been created for this user.

    @Query("SELECT m FROM MarginAccount m WHERE m.marginCallTriggered = true")
    List<MarginAccount> findAllMarginCallAccounts();
    // Used by AdminController to see all users currently
    // under a margin call — critical for risk management.

    @Query("SELECT m FROM MarginAccount m WHERE m.marginUsed > 0")
    List<MarginAccount> findAllActiveMarginUsers();
    // Returns users who are currently using margin.
    // Used by AdminController for margin exposure monitoring.
}