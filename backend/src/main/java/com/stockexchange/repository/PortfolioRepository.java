package com.stockexchange.repository;

import com.stockexchange.entity.Portfolio;
import com.stockexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUser(User user);
    // Primary lookup — every portfolio page load calls this.
    // One user has exactly one portfolio so Optional is correct here.

    Optional<Portfolio> findByUserId(Long userId);
    // Same as above but takes userId directly —
    // avoids fetching the full User object just to find a portfolio.

    boolean existsByUser(User user);
    // Used during user registration to check if a portfolio
    // has already been created for this user.
}