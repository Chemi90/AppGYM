// backend/src/main/java/com/example/gymapp/repository/RpgCharacterRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.RpgCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RpgCharacterRepository extends JpaRepository<RpgCharacter, Long> {
    Optional<RpgCharacter> findByUserId(Long userId);
}
