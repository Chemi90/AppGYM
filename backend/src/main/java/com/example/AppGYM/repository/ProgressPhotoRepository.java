// backend/src/main/java/com/example/AppGYM/repository/ProgressPhotoRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.ProgressPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressPhotoRepository
        extends JpaRepository<ProgressPhoto,Long> {
    List<ProgressPhoto> findByUserIdOrderByDateAsc(Long userId);
}
