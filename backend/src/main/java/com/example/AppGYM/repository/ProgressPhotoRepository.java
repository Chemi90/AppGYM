package com.example.AppGYM.repository;

import com.example.AppGYM.model.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Almacena la URL de la foto, la fecha y su tipo (FRONT / SIDE / BACK)
 */
public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, Long> {

    /* listado completo ordenado por fecha */
    List<ProgressPhoto> findByUserIdOrderByDateAsc(Long userId);

    /* listado dentro de intervalo */
    List<ProgressPhoto> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
