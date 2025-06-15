package com.example.AppGYM.repository;

import com.example.AppGYM.model.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {
    Optional<DailyEntry> findByUserIdAndWorkoutDate(Long userId, LocalDate date);
    List<DailyEntry> findTop30ByUserIdOrderByWorkoutDateDesc(Long userId);
}