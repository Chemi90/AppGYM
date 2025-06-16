package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyStatsRepository extends JpaRepository<BodyStats, Long> {

    /* último registro (para “Medidas actuales”) */
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);

    /* histórico completo, ascendente por fecha */
    List<BodyStats> findByUserIdOrderByDateAsc(Long userId);

    /* histórico acotado por fecha */
    List<BodyStats> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
