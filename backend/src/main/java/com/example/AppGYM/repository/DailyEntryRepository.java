// backend/src/main/java/com/example/AppGYM/repository/DailyEntryRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry,Long> {

    /* para GET rango */
    List<DailyEntry> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);

    /* para POST (upsert) */
    Optional<DailyEntry> findByUserIdAndDate(Long userId, LocalDate date);
}
