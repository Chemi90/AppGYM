package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Mediciones corporales históricas del usuario.
 */
public interface BodyStatsRepository extends JpaRepository<BodyStats, Long> {

    /* Histórico completo ASC */
    List<BodyStats> findByUserIdOrderByDateAsc(Long userId);

    /* Histórico en intervalo ASC */
    List<BodyStats> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);

    /* Última medición (fecha más reciente) */
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);
}
