package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyStatsRepository extends JpaRepository<BodyStats, Long> {

    /* más reciente */
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);

    /* lista completa ordenada */
    List<BodyStats> findByUserIdOrderByDateAsc(Long userId);

    /* lista por intervalo */
    List<BodyStats> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
