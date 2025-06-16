// backend/src/main/java/com/example/AppGYM/repository/DailyEntryRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.DailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    /* todas las entradas de un usuario, ordenadas por fecha */
    @Query("select e from DailyEntry e where e.user.id = :uid order by e.date asc")
    List<DailyEntry> findByUserId(Long uid);

    /* intervalo */
    List<DailyEntry> findByUserIdAndDateBetweenOrderByDateAsc(
            Long uid, LocalDate from, LocalDate to);

    /* una única fecha (para upsert) */
    Optional<DailyEntry> findByUserIdAndDate(Long uid, LocalDate date);
}
