package com.stockexchange.repository;

import com.stockexchange.entity.MarketHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketHoursRepository extends JpaRepository<MarketHours, Long> {

    Optional<MarketHours> findTopByOrderByLastUpdatedAtDesc();
    // Always fetches the most recently updated market hours config.
    // There will only ever be one active config row but this ensures
    // we always get the latest one if admin updates the schedule.
}