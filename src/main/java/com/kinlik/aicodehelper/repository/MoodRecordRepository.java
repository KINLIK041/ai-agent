package com.kinlik.aicodehelper.repository;

import com.kinlik.aicodehelper.entity.MoodRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoodRecordRepository extends JpaRepository<MoodRecord, Long> {
    List<MoodRecord> findByUsernameOrderByRecordDateDesc(String username);
    Optional<MoodRecord> findByUsernameAndRecordDate(String username, LocalDate date);
    List<MoodRecord> findByUsernameAndRecordDateBetween(String username, LocalDate startDate, LocalDate endDate);
}