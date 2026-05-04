package com.kinlik.aicodehelper.repository;

import com.kinlik.aicodehelper.entity.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {
    Optional<WeeklyGoal> findByUsernameAndWeekStart(String username, LocalDate weekStart);
    List<WeeklyGoal> findByUsernameOrderByWeekStartDesc(String username);
}
