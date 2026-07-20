package com.moneylens.repository;

import com.moneylens.model.DailyDigest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyDigestRepository extends JpaRepository<DailyDigest, Long> {
    Optional<DailyDigest> findByUserIdAndDigestDate(Long userId, LocalDate date);
}
