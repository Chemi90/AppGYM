// backend/src/main/java/com/example/AppGYM/repository/BodyStatsRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyStatsRepository extends JpaRepository<BodyStats,Long> {

    /* Último registro (= medidas actuales) */
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);

    /* Histórico completo o entre fechas */
    List<BodyStats> findByUserIdOrderByDateAsc(Long userId);
    List<BodyStats> findByUserIdAndDateBetweenOrderByDateAsc(Long userId,
                                                             LocalDate from,
                                                             LocalDate to);
}
