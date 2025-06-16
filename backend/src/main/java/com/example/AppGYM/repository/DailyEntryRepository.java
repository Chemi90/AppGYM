package com.example.AppGYM.repository;

import com.example.AppGYM.model.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Entrenos diarios.
 */
public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    /* un día concreto (para upsert) */
    Optional<DailyEntry> findByUserIdAndDate(Long userId, LocalDate date);

    /* intervalo de fechas ordenado */
    List<DailyEntry> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
