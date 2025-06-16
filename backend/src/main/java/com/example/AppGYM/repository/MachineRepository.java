// backend/src/main/java/com/example/AppGYM/repository/MachineRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine,Long> {
    Optional<Machine> findByName(String name);
}
