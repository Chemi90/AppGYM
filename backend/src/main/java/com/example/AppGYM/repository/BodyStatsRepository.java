// backend/src/main/java/com/example/AppGYM/repository/BodyStatsRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.BodyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BodyStatsRepository extends JpaRepository<BodyStats,Long> {
    Optional<BodyStats> findTopByUserIdOrderByDateDesc(Long userId);  // último registro
}
