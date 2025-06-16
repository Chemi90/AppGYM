package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyStatsRepository extends JpaRepository<BodyStats, Long> {

    /* última medición */
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);

    /* medición para una fecha concreta */
    Optional<BodyStats> findByUserIdAndDate(Long userId, LocalDate date);

    /* rango de fechas (orden ascendente) */
    List<BodyStats> findByUserIdAndDateBetweenOrderByDateAsc(Long userId,
                                                             LocalDate from,
                                                             LocalDate to);

    /* ===== método que reclamaba PdfService ===== */
    List<BodyStats> findByUserIdOrderByDateAsc(Long userId);
}
