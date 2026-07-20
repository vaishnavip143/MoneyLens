package com.moneylens.repository;

import com.moneylens.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdAndStatus(Long userId, Goal.GoalStatus status);
    List<Goal> findByUserId(Long userId);
}
