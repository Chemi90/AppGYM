package com.example.AppGYM.repository;

import com.example.AppGYM.model.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, Long> {

    List<ProgressPhoto> findByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate from, LocalDate to);
}
