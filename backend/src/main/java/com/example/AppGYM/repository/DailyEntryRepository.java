package com.example.AppGYM.repository;

import com.example.AppGYM.model.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    /*  ─── EXISTENTES ─── */
    List<DailyEntry> findByUserIdOrderByDateAsc(Long userId);

    List<DailyEntry> findByUserIdAndDateBetweenOrderByDateAsc(Long id, LocalDate f, LocalDate t);
    /*  ─── NUEVO  (lo pide DailyEntryController) ─── */
    Optional<DailyEntry> findByUserIdAndDate(Long userId, LocalDate date);
}
